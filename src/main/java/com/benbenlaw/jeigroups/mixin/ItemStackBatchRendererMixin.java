package com.benbenlaw.jeigroups.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mezz.jei.api.ingredients.rendering.BatchRenderElement;
import mezz.jei.library.render.ItemStackRenderer;
import mezz.jei.library.render.batch.SimpleItemStackBatchRenderer;
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
            method = "renderBatch(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lmezz/jei/library/render/ItemStackRenderer;Ljava/util/List;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;II)V"),
            remap = false
    )
    private void jeigroups$drawBackgroundBeforeItem(
            GuiGraphicsExtractor guiGraphics,
            ItemStackRenderer itemStackRenderer,
            List<BatchRenderElement<ItemStack>> elements,
            CallbackInfo ci,
            @Local BatchRenderElement<ItemStack> element
    ) {
        ItemStack stack = element.ingredient();

        if (stack != null && !stack.isEmpty() && jeigroups$isGrouped(stack)) {
            guiGraphics.fill(element.x(), element.y(), element.x() + 16, element.y() + 16, 0x8055FF55);
        }
    }

    @Unique
    private boolean jeigroups$isGrouped(ItemStack stack) {
        // Check our static set in the plugin
        // We use matches() or compare the Item to be safe
        return com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin.EXPANDED_ITEM_STACKS
                .stream()
                .anyMatch(expanded -> ItemStack.isSameItem(stack, expanded));
    }
}