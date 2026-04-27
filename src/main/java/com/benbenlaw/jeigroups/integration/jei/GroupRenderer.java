package com.benbenlaw.jeigroups.integration.jei;

import mezz.jei.api.ingredients.IIngredientRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class GroupRenderer implements IIngredientRenderer<StackGroup> {

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, StackGroup ingredient) {
        guiGraphics.fakeItem(ingredient.icon(), 0, 0);
        String label = ingredient.expanded() ? "-" : "+";
        int color = ingredient.expanded() ? 0xFFFF0000 : 0xFF00FF00;
        guiGraphics.text(Minecraft.getInstance().font, label, 12, 10, color);
        guiGraphics.fill(0, 0, 16, 16, 0x55000000);
    }


    @Override
    public List<Component> getTooltip(StackGroup ingredient, TooltipFlag tooltipFlag) {
        return List.of(
                Component.literal("Group: " + ingredient.name()).withStyle(net.minecraft.ChatFormatting.YELLOW),
                Component.literal("Click to expand").withStyle(net.minecraft.ChatFormatting.GRAY)
        );
    }
}