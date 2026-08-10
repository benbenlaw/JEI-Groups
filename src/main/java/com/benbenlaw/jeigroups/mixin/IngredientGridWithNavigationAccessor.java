package com.benbenlaw.jeigroups.mixin;

import mezz.jei.gui.overlay.ingredients.IngredientGrid;
import mezz.jei.gui.overlay.ingredients.IngredientGridWithNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IngredientGridWithNavigation.class)
public interface IngredientGridWithNavigationAccessor {
    @Accessor("ingredientGrid")
    IngredientGrid jeigroups$getIngredientGrid();
}