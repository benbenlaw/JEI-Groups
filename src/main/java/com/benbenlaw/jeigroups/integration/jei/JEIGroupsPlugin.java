package com.benbenlaw.jeigroups.integration.jei;

import com.benbenlaw.jeigroups.JEIGroups;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@JeiPlugin
public class JEIGroupsPlugin implements IModPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(JEIGroupsPlugin.class);
    public static final IIngredientType<StackGroup> GROUP_TYPE = () -> StackGroup.class;
    public static final Set<ItemStack> EXPANDED_ITEM_STACKS = Collections.synchronizedSet(new HashSet<>());
    private final Map<Item, StackGroup> itemToGroupMap = new HashMap<>();

    public final Map<String, StackGroup> groupCache = new HashMap<>();
    public static JEIGroupsPlugin instance;
    public IJeiRuntime jeiRuntime;

    @Override
    public Identifier getPluginUid() {
        return JEIGroups.identifier("jei_plugin");
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        groupCache.clear();
        itemToGroupMap.clear();

        GroupDataLoader.RAW_DATA.forEach((name, data) -> {
            var iconItem = BuiltInRegistries.ITEM.getValue(data.icon());
            var children = data.items().stream()
                    .map(itemId -> new ItemStack(BuiltInRegistries.ITEM.getValue(itemId)))
                    .toList();

            StackGroup group = new StackGroup(name, new ItemStack(iconItem), children, false);
            groupCache.put(name, group);

            // Map every child item to this group for fast lookup
            for (ItemStack child : children) {
                itemToGroupMap.put(child.getItem(), group);
            }
        });

        //registration.register(GROUP_TYPE, new ArrayList<>(groupCache.values()), new GroupHelper(), new GroupRenderer(), StackGroup.CODEC);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        instance = this;
        this.jeiRuntime = jeiRuntime;
        IIngredientManager manager = jeiRuntime.getIngredientManager();

        for (StackGroup group : groupCache.values()) {
            if (!group.expanded() && group.children().size() > 1) {
                // REMOVE EVERYTHING EXCEPT THE FIRST ONE
                // subList(1, size) handles this safely.
                List<ItemStack> followers = group.children().subList(1, group.children().size());
                manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, followers);
            }
        }
    }

    public void toggleGroup(StackGroup group) {
        IIngredientManager manager = jeiRuntime.getIngredientManager();
        boolean nowExpanded = !group.expanded();
        StackGroup toggledGroup = group.withExpanded(nowExpanded);

        // Update the main cache
        groupCache.put(group.name(), toggledGroup);

        // Update the lookup map for children (important for click detection)
        for (ItemStack child : group.children()) {
            itemToGroupMap.put(child.getItem(), toggledGroup);
        }

        List<ItemStack> followers = group.children().subList(1, group.children().size());

        if (nowExpanded) {
            // Add EVERY item in the group to the highlight set
            for (ItemStack child : group.children()) {
                EXPANDED_ITEM_STACKS.add(child.getItem().getDefaultInstance());
            }
            manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, followers);
        } else {
            // Remove EVERY item from the highlight set
            for (ItemStack child : group.children()) {
                EXPANDED_ITEM_STACKS.remove(child.getItem());
            }
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, followers);
        }

        refreshFilter();
    }

    // Inside JEIGroupsPlugin
    public StackGroup getCollapsedGroupForFirstChild(ItemStack stack) {
        for (StackGroup group : groupCache.values()) {
            if (!group.expanded() && !group.children().isEmpty()) {
                ItemStack anchor = group.children().get(0);
                // Looser check: just check the Item definition
                if (stack.getItem() == anchor.getItem()) {
                    return group;
                }
            }
        }
        return null;
    }

    public StackGroup getGroupFromAnchor(ItemStack stack) {
        for (StackGroup group : groupCache.values()) {
            if (!group.children().isEmpty()) {
                // Check only the base Item definition to be safe
                if (stack.getItem() == group.children().get(0).getItem()) {
                    return group;
                }
            }
        }
        return null;
    }

    public StackGroup getGroupForItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return itemToGroupMap.get(stack.getItem());
    }

    private void refreshFilter() {
        jeiRuntime.getIngredientFilter().setFilterText(jeiRuntime.getIngredientFilter().getFilterText());
    }
}