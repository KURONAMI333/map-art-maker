package com.kuronami.mapartmaker.mapart;

import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the quantiser palette from vanilla map colours.
 *
 * <p>A map pixel is a colour id (0..63) times one of four brightness steps. Colour id 0 renders
 * transparent, so it is excluded here and written directly by {@link ImageQuantizer} for
 * transparent source pixels.
 */
public final class MapPalette {

    private static ImageQuantizer cached;

    private MapPalette() {
    }

    public static synchronized ImageQuantizer quantizer() {
        if (cached == null) {
            cached = build();
        }
        return cached;
    }

    private static ImageQuantizer build() {
        List<Integer> rgb = new ArrayList<>();
        List<Byte> ids = new ArrayList<>();
        for (int id = 1; id < 64; id++) {
            MapColor colour = colourOrNull(id);
            if (colour == null || colour == MapColor.NONE) {
                continue;
            }
            for (MapColor.Brightness brightness : MapColor.Brightness.values()) {
                rgb.add(colour.calculateRGBColor(brightness) & 0xFFFFFF);
                ids.add(colour.getPackedId(brightness));
            }
        }

        int[] rgbArray = new int[rgb.size()];
        byte[] idArray = new byte[ids.size()];
        for (int i = 0; i < rgbArray.length; i++) {
            rgbArray[i] = rgb.get(i);
            idArray[i] = ids.get(i);
        }
        return new ImageQuantizer(rgbArray, idArray);
    }

    /** Vanilla leaves gaps in the colour id range, so unused ids are skipped rather than trusted. */
    private static MapColor colourOrNull(int id) {
        try {
            return MapColor.byId(id);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
