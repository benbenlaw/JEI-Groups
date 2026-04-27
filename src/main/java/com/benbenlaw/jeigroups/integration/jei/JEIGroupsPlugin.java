package com.benbenlaw.jeigroups.integration.jei;

import com.benbenlaw.jeigroups.JEIGroups;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.runtime.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.mojang.text2speech.Narrator.LOGGER;

@JeiPlugin
public class JEIGroupsPlugin implements IModPlugin {

    public static final IIngredientType<StackGroup> GROUP_TYPE = () -> StackGroup.class;
    public static final Set<ItemStack> EXPANDED_ITEM_STACKS = Collections.synchronizedSet(new HashSet<>());

    private final Map<String, StackGroup> groupCache = new HashMap<>();
    public static JEIGroupsPlugin instance;
    public IJeiRuntime jeiRuntime;

    @Override
    public Identifier getPluginUid() {
        return JEIGroups.identifier("jei_plugin");
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {

        groupCache.clear();

        GroupDataLoader.RAW_DATA.forEach((name, data) -> {
            var iconItem = BuiltInRegistries.ITEM.getValue(data.icon());

            var children = data.items().stream()
                    .map(itemId -> new ItemStack(BuiltInRegistries.ITEM.getValue(itemId)))
                    .toList();

            StackGroup group = new StackGroup(name, new ItemStack(iconItem), children, false);
            groupCache.put(name, group);
        });

        if (groupCache.isEmpty()) {
            LOGGER.warn("No JEI groups found in GroupDataLoader!");
        }

        registration.register(
                GROUP_TYPE,
                new ArrayList<>(groupCache.values()),
                new GroupHelper(),
                new GroupRenderer(),
                StackGroup.CODEC
        );
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        instance = this;
        this.jeiRuntime = jeiRuntime;
        IIngredientManager manager = jeiRuntime.getIngredientManager();

        for (StackGroup group : groupCache.values()) {
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, group.children());
        }
    }

    public boolean leftClickWasDown = false;

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
                    IClickableIngredientFactory factory, double mouseX, double mouseY) {
                return Optional.empty();
            }
        });
    }
    public void toggleGroup(StackGroup group) {
        IIngredientManager manager = jeiRuntime.getIngredientManager();
        boolean nowExpanded = !group.expanded();
        StackGroup toggledGroup = group.withExpanded(nowExpanded);

        groupCache.put(group.name(), toggledGroup);

        manager.removeIngredientsAtRuntime(GROUP_TYPE, List.of(group));
        manager.addIngredientsAtRuntime(GROUP_TYPE, List.of(toggledGroup));

        if (nowExpanded) {
            EXPANDED_ITEM_STACKS.addAll(group.children());
            manager.addIngredientsAtRuntime(VanillaTypes.ITEM_STACK, group.children());
        } else {
            group.children().forEach(EXPANDED_ITEM_STACKS::remove);
            manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, group.children());
        }

        refreshFilter();
    }



    private void refreshFilter() {
        jeiRuntime.getIngredientFilter()
                .setFilterText(jeiRuntime.getIngredientFilter().getFilterText());
    }
}