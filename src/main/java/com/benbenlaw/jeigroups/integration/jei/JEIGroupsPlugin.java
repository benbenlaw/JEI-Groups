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
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.*;

@JeiPlugin
public class JEIGroupsPlugin implements IModPlugin {

    public static final IIngredientType<StackGroup> GROUP_TYPE = () -> StackGroup.class;

    private final Map<String, StackGroup> groupCache = new HashMap<>();
    private IJeiRuntime jeiRuntime;

    @Override
    public Identifier getPluginUid() {
        return JEIGroups.identifier("jei_plugin");
    }

    private List<ItemStack> getWoolChildren() {
        return List.of(
                new ItemStack(Items.RED_WOOL), new ItemStack(Items.BLUE_WOOL),
                new ItemStack(Items.GREEN_WOOL), new ItemStack(Items.YELLOW_WOOL),
                new ItemStack(Items.BLACK_WOOL), new ItemStack(Items.WHITE_WOOL),
                new ItemStack(Items.ORANGE_WOOL), new ItemStack(Items.PURPLE_WOOL),
                new ItemStack(Items.BROWN_WOOL), new ItemStack(Items.CYAN_WOOL),
                new ItemStack(Items.LIGHT_GRAY_WOOL), new ItemStack(Items.LIGHT_BLUE_WOOL),
                new ItemStack(Items.MAGENTA_WOOL), new ItemStack(Items.GRAY_WOOL)
        );
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registration) {
        // Starts as expanded = false
        StackGroup woolGroup = new StackGroup("Wool", new ItemStack(Items.BOOK), getWoolChildren(), false);
        groupCache.put(woolGroup.name(), woolGroup);

        registration.register(GROUP_TYPE, List.of(woolGroup), new GroupHelper(), new GroupRenderer(), StackGroup.CODEC);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
        IIngredientManager manager = jeiRuntime.getIngredientManager();

        // Hide the real wools so they ONLY appear when the book is clicked
        manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, getWoolChildren());

        // OPTIONAL: If you want to hide the standard book so the ONLY book is your group
        // manager.removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, List.of(new ItemStack(Items.BOOK)));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory, double mouseX, double mouseY) {
                return jeiRuntime.getIngredientListOverlay().getIngredientUnderMouse()
                        .flatMap(clickable -> {
                            // Since the "Back Button" and "Folder" are both StackGroups now,
                            // this one check handles both Expand and Collapse!
                            if (clickable.getIngredient() instanceof StackGroup group) {
                                toggleGroup(group);
                                return Optional.empty();
                            }
                            return Optional.empty();
                        });
            }
        });
    }

    private void toggleGroup(StackGroup group) {
        IIngredientManager manager = jeiRuntime.getIngredientManager();
        var itemType = VanillaTypes.ITEM_STACK;

        StackGroup toggledGroup = group.withExpanded(!group.expanded());
        groupCache.put(group.name(), toggledGroup);

        if (group.expanded()) {
            // --- COLLAPSING (Shrinking) ---
            manager.addIngredientsAtRuntime(GROUP_TYPE, List.of(toggledGroup));
            manager.removeIngredientsAtRuntime(GROUP_TYPE, List.of(group));
            manager.removeIngredientsAtRuntime(itemType, group.children());
        } else {
            // --- EXPANDING ---
            manager.addIngredientsAtRuntime(GROUP_TYPE, List.of(toggledGroup));
            manager.removeIngredientsAtRuntime(GROUP_TYPE, List.of(group));
            manager.addIngredientsAtRuntime(itemType, group.children());
        }

        // Mandatory refresh
        jeiRuntime.getIngredientFilter().setFilterText(jeiRuntime.getIngredientFilter().getFilterText());
    }
}