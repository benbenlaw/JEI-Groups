package com.benbenlaw.jeigroups.integration.jei;

import com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin;
import com.benbenlaw.jeigroups.integration.jei.StackGroup;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

public class GroupHelper implements IIngredientHelper<StackGroup> {

    @Override
    public IIngredientType<StackGroup> getIngredientType() {
        return JEIGroupsPlugin.GROUP_TYPE;
    }

    @Override
    public String getDisplayName(StackGroup ingredient) {
        return ingredient.name();
    }

    @Override
    public String getUid(StackGroup ingredient, UidContext context) {
        return "jeigroups:" + ingredient.name().toLowerCase() + (ingredient.expanded() ? "_open" : "_closed");
    }

    @Override
    public Identifier getIdentifier(StackGroup ingredient) {
        return Identifier.fromNamespaceAndPath("jeigroups",
                ingredient.name().toLowerCase().replace(" ", "_"));
    }

    // CRITICAL: This tells JEI what to search for.
    // Without this, the filter might hide the folder upon re-adding.
    @Override
    public Iterable<Integer> getColors(StackGroup ingredient) {
        return List.of(); // Optional: used for color search
    }

    @Override
    public ItemStack getCheatItemStack(StackGroup ingredient) {
        return ingredient.icon();
    }

    @Override
    public StackGroup copyIngredient(StackGroup ingredient) {
        return ingredient;
    }

    @Override
    public String getErrorInfo(@Nullable StackGroup ingredient) {
        return ingredient != null ? ingredient.name() : "null group";
    }
}