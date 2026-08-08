package com.kuronami.mapartmaker.mapart;

/**
 * Converts ARGB pixels into vanilla map colour bytes.
 *
 * <p>Deliberately free of Minecraft types: the palette is injected so this can be unit tested
 * without launching the game. {@link MapPalette} builds the real palette from vanilla data.
 */
public final class ImageQuantizer {

    /** Alpha below this is treated as fully transparent and written as colour 0. */
    private static final int ALPHA_CUTOFF = 128;

    private final int[] paletteRgb;
    private final byte[] packedIds;

    /**
     * @param paletteRgb 0xRRGGBB for each candidate colour
     * @param packedIds  the byte vanilla stores in {@code MapItemSavedData.colors}, parallel to {@code paletteRgb}
     */
    public ImageQuantizer(int[] paletteRgb, byte[] packedIds) {
        if (paletteRgb.length != packedIds.length) {
            throw new IllegalArgumentException("palette and id arrays differ in length");
        }
        if (paletteRgb.length == 0) {
            throw new IllegalArgumentException("palette is empty");
        }
        this.paletteRgb = paletteRgb.clone();
        this.packedIds = packedIds.clone();
    }

    public int paletteSize() {
        return paletteRgb.length;
    }

    /**
     * @param pixels ARGB pixels, row-major
     * @param dither apply Floyd–Steinberg error diffusion
     * @return one map colour byte per pixel, row-major
     */
    public byte[] quantize(int[] pixels, int width, int height, boolean dither) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        if (pixels.length != width * height) {
            throw new IllegalArgumentException(
                    "pixel count " + pixels.length + " does not match " + width + "x" + height);
        }

        byte[] out = new byte[pixels.length];
        // Error diffusion needs headroom beyond 0..255, so the working copy is float per channel.
        float[] r = new float[pixels.length];
        float[] g = new float[pixels.length];
        float[] b = new float[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            r[i] = (pixels[i] >> 16) & 0xFF;
            g[i] = (pixels[i] >> 8) & 0xFF;
            b[i] = pixels[i] & 0xFF;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                if (((pixels[i] >>> 24) & 0xFF) < ALPHA_CUTOFF) {
                    out[i] = 0;
                    continue;
                }
                int cr = clamp(r[i]);
                int cg = clamp(g[i]);
                int cb = clamp(b[i]);
                int best = nearest(cr, cg, cb);
                out[i] = packedIds[best];

                if (dither) {
                    int rgb = paletteRgb[best];
                    diffuse(r, g, b, width, height, x, y,
                            cr - ((rgb >> 16) & 0xFF),
                            cg - ((rgb >> 8) & 0xFF),
                            cb - (rgb & 0xFF));
                }
            }
        }
        return out;
    }

    /** Index of the closest palette entry, using the redmean approximation of perceptual distance. */
    int nearest(int r, int g, int b) {
        int best = 0;
        long bestDist = Long.MAX_VALUE;
        for (int i = 0; i < paletteRgb.length; i++) {
            int pr = (paletteRgb[i] >> 16) & 0xFF;
            int pg = (paletteRgb[i] >> 8) & 0xFF;
            int pb = paletteRgb[i] & 0xFF;
            int rmean = (pr + r) / 2;
            int dr = pr - r;
            int dg = pg - g;
            int db = pb - b;
            long dist = (((512 + rmean) * (long) dr * dr) >> 8)
                    + 4L * dg * dg
                    + (((767 - rmean) * (long) db * db) >> 8);
            if (dist < bestDist) {
                bestDist = dist;
                best = i;
                if (dist == 0) {
                    break;
                }
            }
        }
        return best;
    }

    private static void diffuse(float[] r, float[] g, float[] b, int w, int h,
                                int x, int y, float er, float eg, float eb) {
        spread(r, g, b, w, h, x + 1, y, er, eg, eb, 7f / 16f);
        spread(r, g, b, w, h, x - 1, y + 1, er, eg, eb, 3f / 16f);
        spread(r, g, b, w, h, x, y + 1, er, eg, eb, 5f / 16f);
        spread(r, g, b, w, h, x + 1, y + 1, er, eg, eb, 1f / 16f);
    }

    private static void spread(float[] r, float[] g, float[] b, int w, int h,
                               int x, int y, float er, float eg, float eb, float factor) {
        if (x < 0 || x >= w || y < 0 || y >= h) {
            return;
        }
        int i = y * w + x;
        r[i] += er * factor;
        g[i] += eg * factor;
        b[i] += eb * factor;
    }

    private static int clamp(float v) {
        return v < 0f ? 0 : v > 255f ? 255 : Math.round(v);
    }
}
