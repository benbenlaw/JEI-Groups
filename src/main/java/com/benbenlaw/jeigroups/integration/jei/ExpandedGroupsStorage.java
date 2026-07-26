package com.benbenlaw.jeigroups.integration.jei;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class ExpandedGroupsStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Type SET_TYPE = new TypeToken<Set<String>>() {}.getType();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("jeigroups_expanded.json");

    private ExpandedGroupsStorage() {}

    public static Set<String> load() {
        if (!Files.exists(FILE)) return new HashSet<>();

        try (Reader reader = Files.newBufferedReader(FILE)) {
            Set<String> data = GSON.fromJson(reader, SET_TYPE);
            return data != null ? data : new HashSet<>();
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warn("Failed to load expanded JEI Groups state from {}, defaulting to none expanded", FILE, e);
            return new HashSet<>();
        }
    }

    public static void save(Set<String> expandedGroupNames) {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(expandedGroupNames, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to save expanded JEI Groups state to {}", FILE, e);
        }
    }
}