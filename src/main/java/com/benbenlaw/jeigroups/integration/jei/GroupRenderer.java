package com.benbenlaw.jeigroups.integration.jei;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class GroupRenderer implements IIngredientRenderer<StackGroup> {

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, StackGroup ingredient) {
        guiGraphics.fakeItem(ingredient.icon(), 0, 0);

        String label = ingredient.expanded() ? " <" : " +";
        int color = ingredient.expanded() ? 0xFFFF0000 : 0xFF00FF00; // Red for back, Green for expand

        //guiGraphics.pose().pushMatrix();
        //guiGraphics.pose().translate(10, 10, 200);
        //guiGraphics.pose().scale(0.5f, 0.5f, 0.5f);
        //guiGraphics.stra(Minecraft.getInstance().font, label, 0, 0, color, true);
        //guiGraphics.pose().popPose();
    }

    @Override
    public List<Component> getTooltip(StackGroup ingredient, TooltipFlag tooltipFlag) {
        return List.of(
                Component.literal("Group: " + ingredient.name()).withStyle(net.minecraft.ChatFormatting.YELLOW),
                Component.literal("Click to expand").withStyle(net.minecraft.ChatFormatting.GRAY)
        );
    }
}