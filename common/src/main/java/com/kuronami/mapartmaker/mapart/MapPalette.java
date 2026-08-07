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
                rgb.add(rgbOf(colour, brightness));
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

    /**
     * The brightened colour as 0xRRGGBB, the layout {@link ImageQuantizer} expects.
     *
     * <p>Vanilla's own accessor packs the result for the renderer, not for us: on 1.21.1
     * {@code calculateRGBColor} returns 0xAABBGGRR, and on 26.2 the equivalent
     * {@code calculateARGBColor} returns 0xAARRGGBB. Feeding either straight into the quantiser
     * swaps red and blue on one of the two versions. The scaling is reproduced here from
     * {@code col} and {@code modifier}, which are public and identically laid out on both, so the
     * palette is correct on each without a version branch.
     */
    static int rgbOf(MapColor colour, MapColor.Brightness brightness) {
        int modifier = brightness.modifier;
        int r = ((colour.col >> 16) & 0xFF) * modifier / 255;
        int g = ((colour.col >> 8) & 0xFF) * modifier / 255;
        int b = (colour.col & 0xFF) * modifier / 255;
        return (r << 16) | (g << 8) | b;
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
