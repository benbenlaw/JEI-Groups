package com.benbenlaw.jeigroups.integration.jei;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GroupDataLoader extends SimpleJsonResourceReloadListener<StackGroupData> {

    public static final Map<String, StackGroupData> RAW_DATA = new HashMap<>();

    public GroupDataLoader() {
        super(StackGroupData.CODEC, FileToIdConverter.json("jei_groups"));
    }

    @Override
    protected void apply(Map<Identifier, StackGroupData> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        RAW_DATA.clear();
        prepared.forEach((id, data) -> {
            RAW_DATA.put(data.name(), data);
        });
        System.out.println("Loaded " + RAW_DATA.size() + " JEI Group definitions from JSON.");
    }
}