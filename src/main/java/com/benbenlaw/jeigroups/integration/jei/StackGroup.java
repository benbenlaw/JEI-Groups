package com.benbenlaw.jeigroups.integration.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public record StackGroup(String name, ItemStack icon, List<ItemStack> children, boolean expanded, int borderColor, int overlayTint, int plusIconColor, int borderThickness, int backgroundColor) {

    public static final Codec<StackGroup> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(StackGroup::name),
                    ItemStack.CODEC.fieldOf("icon").forGetter(StackGroup::icon),
                    ItemStack.CODEC.listOf().fieldOf("children").forGetter(StackGroup::children),
                    Codec.BOOL.fieldOf("expanded").forGetter(StackGroup::expanded),
                    Codec.INT.fieldOf("borderColor").forGetter(StackGroup::borderColor),
                    Codec.INT.fieldOf("overlayTint").forGetter(StackGroup::overlayTint),
                    Codec.INT.fieldOf("plusIconColor").forGetter(StackGroup::plusIconColor),
                    Codec.INT.fieldOf("borderThickness").forGetter(StackGroup::borderThickness),
                    Codec.INT.fieldOf("backgroundColor").forGetter(StackGroup::backgroundColor)
            ).apply(instance, StackGroup::new)
    );

    public StackGroup withExpanded(boolean state) {
        return new StackGroup(name, icon, children, state, borderColor, overlayTint, plusIconColor, borderThickness, backgroundColor);
    }

    public boolean isAnchor(ItemStack stack) {
        return !children.isEmpty() && stack.getItem() == children.getFirst().getItem();
    }
}