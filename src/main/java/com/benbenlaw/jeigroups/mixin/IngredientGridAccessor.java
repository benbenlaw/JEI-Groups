package com.benbenlaw.jeigroups.mixin;

import mezz.jei.gui.overlay.IngredientGrid;
import mezz.jei.gui.overlay.IngredientListRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IngredientGrid.class)
public interface IngredientGridAccessor {
    @Accessor("ingredientListRenderer")
    IngredientListRenderer jeigroups$getIngredientListRenderer();
}