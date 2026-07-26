package com.benbenlaw.jeigroups;

import com.benbenlaw.jeigroups.integration.jei.GroupDataLoader;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

@Mod(JEIGroups.MOD_ID)
public class JEIGroups {
    public static final String MOD_ID = "jeigroups";

    public JEIGroups(final IEventBus eventBus, final ModContainer modContainer) {
        eventBus.addListener(JEIGroups::onAddReloadListener);
    }

    public static Identifier identifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddClientReloadListenersEvent event) {
        event.addListener(JEIGroups.identifier("jei_groups"), new GroupDataLoader());
    }
}