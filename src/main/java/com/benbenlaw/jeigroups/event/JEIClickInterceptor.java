package com.benbenlaw.jeigroups.event;

import com.benbenlaw.jeigroups.JEIGroups;
import com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin;
import com.benbenlaw.jeigroups.integration.jei.StackGroup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = JEIGroups.MOD_ID, value = Dist.CLIENT)
public class JEIClickInterceptor {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (JEIGroupsPlugin.instance == null || JEIGroupsPlugin.instance.jeiRuntime == null) return;

        var overlay = JEIGroupsPlugin.instance.jeiRuntime.getIngredientListOverlay();
        var ingredient = overlay.getIngredientUnderMouse();

        if (ingredient.isPresent() &&
            ingredient.get().getIngredient() instanceof StackGroup group) {
            JEIGroupsPlugin.instance.toggleGroup(group);
            event.setCanceled(true); // consume the click so JEI never sees it
        }
    }
}