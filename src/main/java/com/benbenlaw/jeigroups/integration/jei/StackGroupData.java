package com.benbenlaw.jeigroups.integration.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.List;

public record StackGroupData(String name, ItemStackTemplate icon, List<Identifier> items) {
    public static final Codec<StackGroupData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("name").forGetter(StackGroupData::name),
            ItemStackTemplate.CODEC.fieldOf("icon").forGetter(StackGroupData::icon),
            Identifier.CODEC.listOf().fieldOf("items").forGetter(StackGroupData::items)
        ).apply(instance, StackGroupData::new)
    );
}