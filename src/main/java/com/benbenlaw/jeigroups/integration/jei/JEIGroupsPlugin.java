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

    private final Map<Item, List<ItemStack>> jeiVariantCache = new HashMap<>();

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
            var iconItem = data.icon().create();

            var children = data.items().stream()
                    .map(itemId -> new ItemStack(BuiltInRegistries.ITEM.getValue(itemId)))
                    .toList();

            StackGroup group = new StackGroup(name, iconItem, children, false);
            groupCache.put(name, group);

            for (ItemStack child : children) {
                itemToGroupMap.put(child.getItem(), group);
            }
        });
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        instance = this;
        this.jeiRuntime = jeiRuntime;

        IIngredientManager manager = jeiRuntime.getIngredientManager();

        Collection<ItemStack> allStacks = manager.getAllIngredients(VanillaTypes.ITEM_STACK);

        for (StackGroup group : groupCache.values()) {
            if (group.children().isEmpty()) continue;

            if (group.children().size() == 1) {
                Item targetItem = group.children().getFirst().getItem();

                List<ItemStack> variants = allStacks.stream()
                        .filter(stack -> stack.getItem() == targetItem)
                        .toList();

                jeiVariantCache.put(targetItem, variants);

                if (!group.expanded() && variants.size() > 1) {
                    ItemStack icon = group.icon();

                    List<ItemStack> toRemove = variants.stream()
                            .filter(stack -> !ItemStack.isSameItemSameComponents(stack, icon))
                            .toList();

                    if (!toRemove.isEmpty()) {
                        manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toRemove);
                    }
                }

            } else {
                List<ItemStack> followers = group.children().subList(1, group.children().size());

                if (!group.expanded() && !followers.isEmpty()) {
                    manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, followers);
                }
            }
        }
    }

    public void toggleGroup(StackGroup group) {
        IIngredientManager manager = jeiRuntime.getIngredientManager();

        boolean nowExpanded = !group.expanded();
        StackGroup toggledGroup = group.withExpanded(nowExpanded);

        groupCache.put(group.name(), toggledGroup);

        for (ItemStack child : group.children()) {
            itemToGroupMap.put(child.getItem(), toggledGroup);
        }

        if (group.children().isEmpty()) return;

        boolean isVariantGroup = group.children().size() == 1;

        if (isVariantGroup) {
            // 🔵 Variant-based (paintings)
            Item targetItem = group.children().getFirst().getItem();

            List<ItemStack> variants = jeiVariantCache.get(targetItem);
            if (variants == null || variants.isEmpty()) return;

            if (nowExpanded) {
                manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, variants);
            } else {
                ItemStack icon = group.icon();

                List<ItemStack> toRemove = variants.stream()
                        .filter(stack -> !ItemStack.isSameItemSameComponents(stack, icon))
                        .toList();

                if (!toRemove.isEmpty()) {
                    manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, toRemove);
                }
            }

        } else {
            // 🟢 Multi-item (wool etc.)
            List<ItemStack> followers = group.children().subList(1, group.children().size());

            if (!followers.isEmpty()) {
                if (nowExpanded) {
                    manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, followers);
                } else {
                    manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, followers);
                }
            }
        }

        refreshFilter();
    }

    public StackGroup getGroupForItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return itemToGroupMap.get(stack.getItem());
    }

    private void refreshFilter() {
        jeiRuntime.getIngredientFilter().setFilterText(
                jeiRuntime.getIngredientFilter().getFilterText()
        );
    }
}