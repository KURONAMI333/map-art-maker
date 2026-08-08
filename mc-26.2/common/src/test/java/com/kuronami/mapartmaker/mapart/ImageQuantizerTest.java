package com.kuronami.mapartmaker.mapart;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageQuantizerTest {

    /** Red, green, blue, white — ids chosen to be distinguishable from array positions. */
    private static ImageQuantizer fixture() {
        int[] rgb = {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFFFF};
        byte[] ids = {10, 20, 30, 40};
        return new ImageQuantizer(rgb, ids);
    }

    private static int argb(int a, int rgb) {
        return (a << 24) | rgb;
    }

    @Test
    void exactPaletteColoursRoundTrip() {
        ImageQuantizer q = fixture();
        int[] pixels = {argb(255, 0xFF0000), argb(255, 0x00FF00), argb(255, 0x0000FF), argb(255, 0xFFFFFF)};
        assertArrayEquals(new byte[]{10, 20, 30, 40}, q.quantize(pixels, 4, 1, false));
    }

    @Test
    void solidImageBecomesOneColour() {
        ImageQuantizer q = fixture();
        int[] pixels = new int[64];
        Arrays.fill(pixels, argb(255, 0xFE0102));
        byte[] out = q.quantize(pixels, 8, 8, false);
        for (byte b : out) {
            assertEquals(10, b, "near-red must quantise to the red entry");
        }
    }

    @Test
    void transparentPixelsBecomeColourZero() {
        ImageQuantizer q = fixture();
        int[] pixels = {argb(0, 0xFF0000), argb(127, 0x00FF00), argb(128, 0x0000FF)};
        byte[] out = q.quantize(pixels, 3, 1, false);
        assertEquals(0, out[0], "alpha 0 is transparent");
        assertEquals(0, out[1], "alpha below the cutoff is transparent");
        assertEquals(30, out[2], "alpha at the cutoff is opaque");
    }

    @Test
    void ditheringPreservesDimensionsAndUsesPaletteOnly() {
        ImageQuantizer q = fixture();
        int[] pixels = new int[32 * 32];
        Arrays.fill(pixels, argb(255, 0x808080));
        byte[] out = q.quantize(pixels, 32, 32, true);

        assertEquals(32 * 32, out.length);
        for (byte b : out) {
            assertTrue(b == 10 || b == 20 || b == 30 || b == 40,
                    "dithering must only emit palette ids, got " + b);
        }
    }

    @Test
    void ditheringMixesColoursThatNoSingleEntryMatches() {
        ImageQuantizer q = fixture();
        int[] pixels = new int[16 * 16];
        Arrays.fill(pixels, argb(255, 0x808080));

        byte[] flat = q.quantize(pixels, 16, 16, false);
        byte[] dithered = q.quantize(pixels, 16, 16, true);

        long flatDistinct = Arrays.stream(toInts(flat)).distinct().count();
        long ditheredDistinct = Arrays.stream(toInts(dithered)).distinct().count();
        assertEquals(1, flatDistinct, "without dithering a flat colour maps to a single entry");
        assertTrue(ditheredDistinct > 1, "dithering should spread error across more than one entry");
    }

    @Test
    void rejectsMismatchedDimensions() {
        ImageQuantizer q = fixture();
        assertThrows(IllegalArgumentException.class, () -> q.quantize(new int[5], 2, 2, false));
        assertThrows(IllegalArgumentException.class, () -> q.quantize(new int[0], 0, 0, false));
    }

    @Test
    void rejectsMalformedPalette() {
        assertThrows(IllegalArgumentException.class,
                () -> new ImageQuantizer(new int[]{0xFF0000}, new byte[]{1, 2}));
        assertThrows(IllegalArgumentException.class,
                () -> new ImageQuantizer(new int[0], new byte[0]));
    }

    @Test
    void nearestPrefersPerceptuallyCloserEntry() {
        ImageQuantizer q = fixture();
        // Dark grey sits between black-ish red and white; the redmean metric must not pick white.
        assertEquals(0, q.nearest(0x40, 0x00, 0x00));
        assertEquals(3, q.nearest(0xF0, 0xF0, 0xF0));
    }

    private static int[] toInts(byte[] bytes) {
        int[] out = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = bytes[i];
        }
        return out;
    }
}
