package com.benbenlaw.jeigroups.mixin;

import com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin;
import com.benbenlaw.jeigroups.integration.jei.StackGroup;
import com.llamalad7.mixinextras.sugar.Local;
import mezz.jei.api.ingredients.rendering.BatchRenderElement;
import mezz.jei.library.render.ItemStackRenderer;
import mezz.jei.library.render.batch.SimpleItemStackBatchRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SimpleItemStackBatchRenderer.class)
public class ItemStackBatchRendererMixin {

    @Inject(
            method = "renderBatch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;II)V", shift = At.Shift.AFTER),
            remap = false
    )
    private void jeigroups$drawOverlay(GuiGraphicsExtractor guiGraphics, ItemStackRenderer itemStackRenderer, List<BatchRenderElement<ItemStack>> elements, CallbackInfo ci, @Local BatchRenderElement<ItemStack> element) {
        ItemStack stack = element.ingredient();
        if (stack == null || stack.isEmpty() || JEIGroupsPlugin.instance == null) return;

        StackGroup group = JEIGroupsPlugin.instance.getGroupForItem(stack);

        if (group != null) {
            if (!group.expanded()) {
                if (stack.getItem() == group.children().get(0).getItem()) {
                    guiGraphics.fakeItem(group.icon(), element.x(), element.y());
                    guiGraphics.fill(element.x(), element.y(), element.x() + 16, element.y() + 16, 0x55000000);
                    guiGraphics.text(Minecraft.getInstance().font, "+", element.x() + 11, element.y() + 9, 0xFF55FF55);
                }
            } else {
                guiGraphics.fill(element.x(), element.y(), element.x() + 16, element.y() + 16, 0x3055FF55);
            }
        }
    }
}