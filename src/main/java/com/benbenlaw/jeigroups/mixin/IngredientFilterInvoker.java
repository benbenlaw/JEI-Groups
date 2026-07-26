package com.benbenlaw.jeigroups.mixin;

import mezz.jei.gui.ingredients.IngredientFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(IngredientFilter.class)
public interface IngredientFilterInvoker {
    @Invoker("notifyListenersOfChange")
    void jeigroups$notifyListenersOfChange();
}