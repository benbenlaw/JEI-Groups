package com.benbenlaw.jeigroups.mixin;

import com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin;
import com.benbenlaw.jeigroups.integration.jei.StackGroup;
import mezz.jei.common.config.IClientConfig;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.gui.config.IngredientTypeSortingConfig;
import mezz.jei.gui.config.ModNameSortingConfig;
import mezz.jei.gui.ingredients.IListElementInfo;
import mezz.jei.gui.ingredients.IListElement;
import mezz.jei.gui.ingredients.IngredientSorter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(IngredientSorter.class)
public class IngredientSorterMixin {

    @Inject(method = "sortIngredients", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V", shift = At.Shift.AFTER), remap = false )

    private static void jeigroups$clusterExpandedGroups(IClientConfig clientConfig, ModNameSortingConfig modNameSortingConfig, IngredientTypeSortingConfig ingredientTypeSortingConfig,
            IIngredientManager ingredientManager, List<IListElementInfo<?>> ingredients, CallbackInfoReturnable<Comparator<IListElement<?>>> cir) {

        JEIGroupsPlugin plugin = JEIGroupsPlugin.instance;
        if (plugin == null) return;

        Map<Item, List<IListElementInfo<?>>> pendingFollowers = new HashMap<>();
        Set<Item> seenVariantAnchor = new HashSet<>();
        List<IListElementInfo<?>> ordered = new ArrayList<>(ingredients.size());

        for (IListElementInfo<?> info : ingredients) {
            ItemStack stack = jeigroups$asItemStack(info);
            if (stack == null) {
                ordered.add(info);
                continue;
            }

            StackGroup group = plugin.getGroupForItem(stack);
            if (group == null) {
                ordered.add(info);
                continue;
            }

            if (group.children().size() >= 2) {
                if (group.isAnchor(stack)) {
                    ordered.add(info);
                } else {
                    Item anchorItem = group.children().getFirst().getItem();
                    pendingFollowers.computeIfAbsent(anchorItem, k -> new ArrayList<>()).add(info);
                }
            } else {
                Item targetItem = stack.getItem();
                if (seenVariantAnchor.add(targetItem)) {
                    ordered.add(info);
                } else {
                    pendingFollowers.computeIfAbsent(targetItem, k -> new ArrayList<>()).add(info);
                }
            }
        }

        if (pendingFollowers.isEmpty()) return;

        List<IListElementInfo<?>> rebuilt = new ArrayList<>(ingredients.size());
        for (IListElementInfo<?> info : ordered) {
            rebuilt.add(info);
            ItemStack stack = jeigroups$asItemStack(info);
            if (stack != null) {
                List<IListElementInfo<?>> followers = pendingFollowers.remove(stack.getItem());
                if (followers != null) {
                    rebuilt.addAll(followers);
                }
            }
        }

        ingredients.clear();
        ingredients.addAll(rebuilt);
    }

    private static ItemStack jeigroups$asItemStack(IListElementInfo<?> info) {
        Object ingredient = info.getTypedIngredient().getIngredient();
        return ingredient instanceof ItemStack stack ? stack : null;
    }
}