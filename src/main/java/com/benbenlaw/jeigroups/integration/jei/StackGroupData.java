package com.benbenlaw.jeigroups.integration.jei;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.List;

public record StackGroupData(String name, ItemStackTemplate icon, List<String> items, int borderColor, int overlayTint, int plusIconColor, int borderThickness, int backgroundColor) {

    private static final int DEFAULT_BORDER_COLOR = 0xFFFFA500;
    private static final int DEFAULT_OVERLAY_TINT = 0x55000000;
    private static final int DEFAULT_PLUS_ICON_COLOR = 0xFF55FF55;
    private static final int DEFAULT_BORDER_THICKNESS = 2;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x00000000;

    private static final Codec<Integer> COLOR_CODEC = Codec.STRING.comapFlatMap(
            StackGroupData::parseColor,
            color -> String.format("#%08X", color)
    );

    private static DataResult<Integer> parseColor(String input) {
        try {
            String hex = input.startsWith("#") ? input.substring(1) : input;
            long parsed = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
                parsed |= 0xFF000000L;
            }
            return DataResult.success((int) parsed);
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Invalid color hex string: " + input);
        }
    }

    public static final Codec<StackGroupData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(StackGroupData::name),
                    ItemStackTemplate.CODEC.fieldOf("icon").forGetter(StackGroupData::icon),
                    Codec.STRING.listOf().fieldOf("items").forGetter(StackGroupData::items),
                    COLOR_CODEC.optionalFieldOf("border_color", DEFAULT_BORDER_COLOR).forGetter(StackGroupData::borderColor),
                    COLOR_CODEC.optionalFieldOf("overlay_tint", DEFAULT_OVERLAY_TINT).forGetter(StackGroupData::overlayTint),
                    COLOR_CODEC.optionalFieldOf("plus_icon_color", DEFAULT_PLUS_ICON_COLOR).forGetter(StackGroupData::plusIconColor),
                    Codec.INT.optionalFieldOf("border_thickness", DEFAULT_BORDER_THICKNESS).forGetter(StackGroupData::borderThickness),
                    COLOR_CODEC.optionalFieldOf("background_color", DEFAULT_BACKGROUND_COLOR).forGetter(StackGroupData::backgroundColor)
            ).apply(instance, StackGroupData::new)
    );
}