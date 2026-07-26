package com.benbenlaw.jeigroups.integration.jei;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class GroupDataLoader extends SimpleJsonResourceReloadListener<StackGroupData> {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Map<String, StackGroupData> RAW_DATA = new HashMap<>();

    public GroupDataLoader() {
        super(StackGroupData.CODEC, FileToIdConverter.json("jei_groups"));
    }

    @Override
    protected void apply(Map<Identifier, StackGroupData> prepared, ResourceManager resourceManager, ProfilerFiller profiler) {
        RAW_DATA.clear();
        prepared.forEach((id, data) -> RAW_DATA.put(data.name(), data));
        LOGGER.info("Loaded {} JEI Group definitions from JSON.", RAW_DATA.size());

        if (JEIGroupsPlugin.instance != null) {
            JEIGroupsPlugin.instance.rebuildGroups();
        }
    }
}