package com.benbenlaw.jeigroups.mixin;

import com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin;
import com.benbenlaw.jeigroups.integration.jei.StackGroup;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.gui.ingredients.IngredientFilter;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Mixin(IngredientFilter.class)
public class IngredientFilterMixin {

    @ModifyReturnValue(method = "getIngredientListUncached", at = @At("RETURN"), remap = false)
    private Stream<ITypedIngredient<?>> jeigroups$collapseGroups(Stream<ITypedIngredient<?>> original, String filterText) {
        JEIGroupsPlugin plugin = JEIGroupsPlugin.instance;
        if (plugin == null) return original;

        List<ITypedIngredient<?>> ordered = original.toList();

        Set<StackGroup> shownGroups = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ITypedIngredient<?>> result = new ArrayList<>(ordered.size());

        for (ITypedIngredient<?> ingredient : ordered) {
            Object raw = ingredient.getIngredient();
            if (!(raw instanceof ItemStack stack)) {
                result.add(ingredient);
                continue;
            }

            StackGroup group = plugin.getGroupForItem(stack);
            if (group == null || group.expanded() || shownGroups.add(group)) {
                result.add(ingredient);
            }
        }

        return result.stream();
    }
}