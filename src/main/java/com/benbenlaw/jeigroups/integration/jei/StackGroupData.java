package com.benbenlaw.jeigroups.integration.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record StackGroupData(String name, Identifier icon, List<Identifier> items) {
    public static final Codec<StackGroupData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("name").forGetter(StackGroupData::name),
            Identifier.CODEC.fieldOf("icon").forGetter(StackGroupData::icon),
            Identifier.CODEC.listOf().fieldOf("items").forGetter(StackGroupData::items)
        ).apply(instance, StackGroupData::new)
    );
}