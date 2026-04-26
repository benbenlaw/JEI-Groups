package com.benbenlaw.jeigroups.integration.jei;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
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

public class GroupDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static Map<String, StackGroup> RAW_GROUPS = new HashMap<>();

    public GroupDataLoader() {
        super(GSON, "jei_groups");
    }

    @Override
    protected void apply(Object object, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        RAW_GROUPS.clear();
        object.forEach((id, json) -> {
            JsonObject obj = json.getAsJsonObject();
            String name = obj.get("name").getAsString();
            Item iconItem = BuiltInRegistries.ITEM.getValue(Identifier.parse(obj.get("icon").getAsString()));

            List<ItemStack> children = new ArrayList<>();
            obj.getAsJsonArray("items").forEach(element -> {
                Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(element.getAsString()));
                children.add(new ItemStack(item));
            });

            RAW_GROUPS.put(name, new StackGroup(name, new ItemStack(iconItem), children, false));
        });
    }


}