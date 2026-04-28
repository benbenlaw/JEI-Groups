package com.benbenlaw.jeigroups.event;

import com.benbenlaw.jeigroups.JEIGroups;
import com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin;
import com.benbenlaw.jeigroups.integration.jei.StackGroup;
import mezz.jei.api.ingredients.ITypedIngredient;
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

import java.util.Optional;

import static com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin.instance;

@EventBusSubscriber(modid = JEIGroups.MOD_ID, value = Dist.CLIENT)
public class JEIClickInterceptor {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        if (instance == null || instance.jeiRuntime == null) return;

        var overlay = instance.jeiRuntime.getIngredientListOverlay();
        var ingredient = overlay.getIngredientUnderMouse();

        if (ingredient.isPresent() && ingredient.get().getIngredient() instanceof ItemStack stack) {
            StackGroup group = instance.getGroupForItem(stack);

            if (group != null) {
                boolean isShiftDown = GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

                boolean isAnchor = stack.getItem() == group.children().getFirst().getItem();

                if (!group.expanded()) {
                    if (isAnchor) {
                        instance.toggleGroup(group);
                        event.setCanceled(true);
                    }
                } else {
                    if (isShiftDown) {
                        instance.toggleGroup(group);
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (instance == null || instance.jeiRuntime == null) return;

        ItemStack stack = event.getItemStack();
        StackGroup group = instance.getGroupForItem(stack);

        if (group != null) {
            boolean isAnchor = stack.getItem() == group.children().getFirst().getItem();

            if (!group.expanded()) {
                if (isAnchor) {
                    event.getToolTip().set(0, Component.translatable("tooltip.jeigroups.group", group.name())
                            .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD));

                    int count = group.children().size() - 1;
                    event.getToolTip().add(Component.translatable("tooltip.jeigroups.click_to_expand", count)
                            .withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));

                }
            } else {
                event.getToolTip().add(Component.translatable("tooltip.jeigroups.click_to_collapse", group.name())
                        .withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC));
            }
        }
    }
}