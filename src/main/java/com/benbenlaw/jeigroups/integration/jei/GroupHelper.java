package com.benbenlaw.jeigroups.integration.jei;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

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
    public Object getUid(StackGroup ingredient, UidContext context) {
        return "jeigroups:" + ingredient.name().toLowerCase() + (ingredient.expanded() ? "_expanded" : "_collapsed");
    }

    @Override
    public Identifier getIdentifier(StackGroup ingredient) {
        return Identifier.fromNamespaceAndPath("jeigroups", "folder/" + ingredient.name().toLowerCase());
    }

    @Override
    public StackGroup copyIngredient(StackGroup ingredient) {
        return ingredient;
    }

    @Override
    public String getErrorInfo(@Nullable StackGroup ingredient) {
        return ingredient != null ? "Group: " + ingredient.name() : "null group";
    }

    @Override
    public String getDisplayModId(StackGroup ingredient) {
        return "JEI Groups";
    }
}