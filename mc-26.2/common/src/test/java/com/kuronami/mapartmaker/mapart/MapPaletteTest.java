package com.kuronami.mapartmaker.mapart;

import net.minecraft.world.level.material.MapColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the palette to 0xRRGGBB.
 *
 * <p>{@link ImageQuantizer} compares palette entries against source pixels read with
 * {@code BufferedImage.getRGB}, which is 0xRRGGBB. Vanilla's own colour accessor does not use that
 * layout on every version, so the channel order is asserted rather than assumed. Ordering is
 * checked against colours whose channels are far apart, which catches a red/blue swap without
 * depending on the brightness constants.
 */
class MapPaletteTest {

    @Test
    void redStaysInTheRedChannel() {
        // COLOR_RED is 0xB02E26: red dwarfs the other two, so a swapped layout is unmistakable.
        int packed = MapPalette.rgbOf(MapColor.COLOR_RED, MapColor.Brightness.NORMAL);
        int r = (packed >> 16) & 0xFF;
        int g = (packed >> 8) & 0xFF;
        int b = packed & 0xFF;

        assertTrue(r > g && r > b,
                () -> "red should dominate but got r=" + r + " g=" + g + " b=" + b
                        + " (blue dominating means the palette is BGR)");
    }

    @Test
    void blueStaysInTheBlueChannel() {
        // COLOR_BLUE is 0x4040CC: blue dominates, the mirror image of the check above.
        int packed = MapPalette.rgbOf(MapColor.COLOR_BLUE, MapColor.Brightness.NORMAL);
        int r = (packed >> 16) & 0xFF;
        int b = packed & 0xFF;

        assertTrue(b > r, () -> "blue should dominate but got r=" + r + " b=" + b);
    }

    @Test
    void everyColourScalesItsOwnChannels() {
        for (int id = 1; id < 64; id++) {
            MapColor colour;
            try {
                colour = MapColor.byId(id);
            } catch (RuntimeException e) {
                continue;
            }
            if (colour == MapColor.NONE) {
                continue;
            }
            for (MapColor.Brightness brightness : MapColor.Brightness.values()) {
                int packed = MapPalette.rgbOf(colour, brightness);
                int m = brightness.modifier;
                assertEquals(((colour.col >> 16) & 0xFF) * m / 255, (packed >> 16) & 0xFF,
                        "red for id " + id + " at " + brightness);
                assertEquals(((colour.col >> 8) & 0xFF) * m / 255, (packed >> 8) & 0xFF,
                        "green for id " + id + " at " + brightness);
                assertEquals((colour.col & 0xFF) * m / 255, packed & 0xFF,
                        "blue for id " + id + " at " + brightness);
            }
        }
    }

    @Test
    void brightnessDarkensEveryChannel() {
        int normal = MapPalette.rgbOf(MapColor.GRASS, MapColor.Brightness.NORMAL);
        int lowest = MapPalette.rgbOf(MapColor.GRASS, MapColor.Brightness.LOWEST);

        for (int shift = 0; shift <= 16; shift += 8) {
            int n = (normal >> shift) & 0xFF;
            int l = (lowest >> shift) & 0xFF;
            assertTrue(l < n, "channel at shift " + shift + " should darken: " + l + " !< " + n);
        }
    }

    @Test
    void paletteCoversEveryColourAndBrightness() {
        ImageQuantizer quantizer = MapPalette.quantizer();
        // Vanilla leaves gaps in the id range, so this is a floor rather than an exact count.
        assertTrue(quantizer.paletteSize() >= 4 * 50,
                "palette looks truncated: " + quantizer.paletteSize());
        assertEquals(0, quantizer.paletteSize() % 4, "every colour should contribute 4 brightnesses");
    }
}
