package com.benbenlaw.jeigroups.event;

import com.benbenlaw.jeigroups.JEIGroups;
import com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin;
import com.benbenlaw.jeigroups.integration.jei.StackGroup;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import static com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin.instance;

@EventBusSubscriber(modid = JEIGroups.MOD_ID, value = Dist.CLIENT)
public class JEIClickInterceptor {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (instance == null || instance.jeiRuntime == null) return;

        var overlay = instance.jeiRuntime.getIngredientListOverlay();
        var ingredient = overlay.getIngredientUnderMouse();

        if (ingredient.isEmpty() || !(ingredient.get().getIngredient() instanceof ItemStack stack)) return;

        StackGroup group = instance.getGroupForItem(stack);
        if (group == null) return;

        if (!group.expanded()) {
            instance.toggleGroup(group);
            event.setCanceled(true);
        } else if (isShiftDown()) {
            instance.toggleGroup(group);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (instance == null || instance.jeiRuntime == null) return;

        ItemStack stack = event.getItemStack();
        StackGroup group = instance.getGroupForItem(stack);
        if (group == null) return;

        var overlay = instance.jeiRuntime.getIngredientListOverlay();
        var underMouse = overlay.getIngredientUnderMouse();
        if (underMouse.isEmpty() || !(underMouse.get().getIngredient() instanceof ItemStack hoveredStack)
                || !ItemStack.isSameItemSameComponents(hoveredStack, stack)) {
            return;
        }

        if (!group.expanded()) {
            event.getToolTip().set(0, Component.translatable("tooltip.jeigroups.group", group.name())
                    .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD));

            int count = instance.getGroupMemberCount(group) - 1;
            event.getToolTip().add(Component.translatable("tooltip.jeigroups.click_to_expand", count)
                    .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.ITALIC));
        } else {
            event.getToolTip().add(Component.translatable("tooltip.jeigroups.click_to_collapse", group.name())
                    .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.ITALIC));
        }
    }

    private static boolean isShiftDown() {
        long window = Minecraft.getInstance().getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }
}