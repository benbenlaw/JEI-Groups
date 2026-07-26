package com.benbenlaw.jeigroups.mixin;

import com.benbenlaw.jeigroups.integration.jei.JEIGroupsPlugin;
import com.benbenlaw.jeigroups.integration.jei.StackGroup;
import com.llamalad7.mixinextras.sugar.Local;
import mezz.jei.api.ingredients.rendering.BatchRenderElement;
import mezz.jei.library.render.ItemStackRenderer;
import mezz.jei.library.render.batch.SimpleItemStackBatchRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(SimpleItemStackBatchRenderer.class)
public class ItemStackBatchRendererMixin {

    @Unique
    private static final int ICON_SIZE = 16;

    @Inject(method = "renderBatch", at = @At("HEAD"))
    private void jeigroups$drawGroupBackgrounds(GuiGraphicsExtractor guiGraphics, ItemStackRenderer itemStackRenderer, List<BatchRenderElement<ItemStack>> elements, CallbackInfo ci) {
        JEIGroupsPlugin plugin = JEIGroupsPlugin.instance;
        if (plugin == null || elements.isEmpty()) return;

        Map<StackGroup, List<int[]>> cellsByGroup = jeigroups$collectExpandedGroupCells(plugin, elements);
        if (cellsByGroup.isEmpty()) return;

        int[] spacing = computeGridSpacing(elements);
        int padX = Math.max(1, (spacing[0] - ICON_SIZE) / 2);
        int padY = Math.max(1, (spacing[1] - ICON_SIZE) / 2);

        for (var entry : cellsByGroup.entrySet()) {
            StackGroup group = entry.getKey();
            int bg = group.backgroundColor();
            if ((bg >>> 24) == 0) continue;

            for (int[] run : mergeIntoRowRuns(entry.getValue(), spacing[0])) {
                guiGraphics.fill(run[1] - padX, run[0] - padY, run[2] + ICON_SIZE + padX, run[0] + ICON_SIZE + padY, bg);
            }
        }
    }

    private List<int[]> mergeIntoRowRuns(List<int[]> cells, int columnSpacing) {
        Map<Integer, List<Integer>> byRow = new TreeMap<>();
        for (int[] cell : cells) {
            byRow.computeIfAbsent(cell[1], y -> new ArrayList<>()).add(cell[0]);
        }

        List<int[]> runs = new ArrayList<>();
        for (var rowEntry : byRow.entrySet()) {
            int rowY = rowEntry.getKey();
            List<Integer> xs = rowEntry.getValue();
            Collections.sort(xs);

            int runStart = xs.getFirst();
            int prev = runStart;
            for (int i = 1; i < xs.size(); i++) {
                int x = xs.get(i);
                if (x - prev != columnSpacing) {
                    runs.add(new int[]{rowY, runStart, prev});
                    runStart = x;
                }
                prev = x;
            }
            runs.add(new int[]{rowY, runStart, prev});
        }
        return runs;
    }

    @Inject(method = "renderBatch", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;II)V", shift = At.Shift.AFTER), remap = false)
    private void jeigroups$drawAnchorOverlay(GuiGraphicsExtractor guiGraphics, ItemStackRenderer itemStackRenderer, List<BatchRenderElement<ItemStack>> elements, CallbackInfo ci, @Local BatchRenderElement<ItemStack> element) {
        ItemStack stack = element.ingredient();
        if (stack == null || stack.isEmpty() || JEIGroupsPlugin.instance == null) return;

        JEIGroupsPlugin plugin = JEIGroupsPlugin.instance;
        StackGroup group = plugin.getGroupForItem(stack);
        if (group == null || group.expanded()) return;

        int[] spacing = computeGridSpacing(elements);
        int padX = Math.max(1, (spacing[0] - ICON_SIZE) / 2);
        int padY = Math.max(1, (spacing[1] - ICON_SIZE) / 2);

        guiGraphics.fakeItem(group.icon(), element.x(), element.y());
        guiGraphics.fill(
                element.x() - padX, element.y() - padY,
                element.x() + ICON_SIZE + padX, element.y() + ICON_SIZE + padY,
                group.overlayTint()
        );
        guiGraphics.text(Minecraft.getInstance().font, "+", element.x() + 11, element.y() + 9, group.plusIconColor());

        List<int[]> singleCell = List.of(new int[]{element.x(), element.y()});
        drawMergedOutline(guiGraphics, singleCell, padX, padY, group.borderColor(), group.borderThickness());
    }

    // --- Once per batch, after items render: merged outline around each expanded group's cells ---
    @Inject(method = "renderBatch", at = @At("RETURN"), remap = false)
    private void jeigroups$drawGroupOutlines(GuiGraphicsExtractor guiGraphics, ItemStackRenderer itemStackRenderer, List<BatchRenderElement<ItemStack>> elements, CallbackInfo ci) {
        JEIGroupsPlugin plugin = JEIGroupsPlugin.instance;
        if (plugin == null || elements.isEmpty()) return;

        Map<StackGroup, List<int[]>> cellsByGroup = jeigroups$collectExpandedGroupCells(plugin, elements);
        if (cellsByGroup.isEmpty()) return;

        int[] spacing = computeGridSpacing(elements);
        int padX = Math.max(1, (spacing[0] - ICON_SIZE) / 2);
        int padY = Math.max(1, (spacing[1] - ICON_SIZE) / 2);

        for (var entry : cellsByGroup.entrySet()) {
            StackGroup group = entry.getKey();
            drawMergedOutline(guiGraphics, entry.getValue(), padX, padY, group.borderColor(), group.borderThickness());
        }
    }

    private Map<StackGroup, List<int[]>> jeigroups$collectExpandedGroupCells(JEIGroupsPlugin plugin, List<BatchRenderElement<ItemStack>> elements) {
        Map<StackGroup, List<int[]>> cellsByGroup = new IdentityHashMap<>();
        for (BatchRenderElement<ItemStack> element : elements) {
            ItemStack stack = element.ingredient();
            if (stack.isEmpty()) continue;

            StackGroup group = plugin.getGroupForItem(stack);
            if (group == null || !group.expanded()) continue;

            cellsByGroup.computeIfAbsent(group, g -> new ArrayList<>()).add(new int[]{element.x(), element.y()});
        }
        return cellsByGroup;
    }

    private int[] computeGridSpacing(List<BatchRenderElement<ItemStack>> elements) {
        SortedSet<Integer> xs = new TreeSet<>();
        SortedSet<Integer> ys = new TreeSet<>();
        for (BatchRenderElement<ItemStack> e : elements) {
            xs.add(e.x());
            ys.add(e.y());
        }
        int dx = smallestPositiveGap(xs);
        int dy = smallestPositiveGap(ys);
        return new int[]{dx > 0 ? dx : ICON_SIZE + 2, dy > 0 ? dy : ICON_SIZE + 2};
    }

    private int smallestPositiveGap(SortedSet<Integer> values) {
        Integer prev = null;
        int smallest = -1;
        for (int v : values) {
            if (prev != null) {
                int gap = v - prev;
                if (gap > 0 && (smallest < 0 || gap < smallest)) smallest = gap;
            }
            prev = v;
        }
        return smallest;
    }

    private enum Side { TOP, BOTTOM, LEFT, RIGHT }
    private record Edge(int x1, int y1, int x2, int y2) {}

    private void drawMergedOutline(GuiGraphicsExtractor guiGraphics, List<int[]> cells, int padX, int padY, int borderColor, int thickness) {
        Map<Edge, List<Side>> edgeSides = new HashMap<>();

        for (int[] cell : cells) {
            int x1 = cell[0] - padX;
            int y1 = cell[1] - padY;
            int x2 = cell[0] + ICON_SIZE + padX;
            int y2 = cell[1] + ICON_SIZE + padY;

            edgeSides.computeIfAbsent(new Edge(x1, y1, x2, y1), k -> new ArrayList<>()).add(Side.TOP);
            edgeSides.computeIfAbsent(new Edge(x1, y2, x2, y2), k -> new ArrayList<>()).add(Side.BOTTOM);
            edgeSides.computeIfAbsent(new Edge(x1, y1, x1, y2), k -> new ArrayList<>()).add(Side.LEFT);
            edgeSides.computeIfAbsent(new Edge(x2, y1, x2, y2), k -> new ArrayList<>()).add(Side.RIGHT);
        }

        int t = Math.max(1, thickness);
        for (var entry : edgeSides.entrySet()) {
            List<Side> sides = entry.getValue();
            if (sides.size() != 1) continue;

            Edge e = entry.getKey();
            switch (sides.getFirst()) {
                case TOP -> guiGraphics.fill(e.x1(), e.y1(), e.x2(), e.y1() + t, borderColor);
                case BOTTOM -> guiGraphics.fill(e.x1(), e.y2() - t, e.x2(), e.y2(), borderColor);
                case LEFT -> guiGraphics.fill(e.x1(), e.y1(), e.x1() + t, e.y2(), borderColor);
                case RIGHT -> guiGraphics.fill(e.x2() - t, e.y1(), e.x2(), e.y2(), borderColor);
            }
        }
    }
}