package com.kuronami.mapartmaker.mapart;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Client-side mirror of the URL path: the same decode, scale and quantise steps as
 * {@link ImageFetcher} and {@link MapArtService#quantizeTiles}, so a file dropped on the GUI and a
 * URL pointing at the identical image land on identical map colour bytes.
 *
 * <p>No network or Minecraft-world types here, so this runs off the render thread without
 * touching anything that requires the main thread.
 */
public final class LocalMapArtEncoder {

    private LocalMapArtEncoder() {
    }

    /** Carries a translation key rather than a literal message, unlike {@link ImageFetcher.FetchException}. */
    public static final class LocalImageException extends Exception {
        public LocalImageException(String translationKey) {
            super(translationKey);
        }

        public String translationKey() {
            return getMessage();
        }
    }

    /**
     * @param fileBytes raw bytes read from the dropped file
     * @return one colour-byte tile per grid cell, reading order, ready for {@link MapArtService#createMap}
     */
    public static byte[][] encode(byte[] fileBytes, int tilesX, int tilesY, boolean dither) throws LocalImageException {
        if (fileBytes.length > ImageFetcher.MAX_BYTES) {
            throw new LocalImageException("message.map_art_maker.drop_too_large");
        }

        BufferedImage source;
        try {
            source = ImageFetcher.decodeRaw(fileBytes);
        } catch (IOException e) {
            throw new LocalImageException("message.map_art_maker.drop_not_image");
        }
        if (source == null) {
            throw new LocalImageException("message.map_art_maker.drop_not_image");
        }

        int[] pixels = ImageFetcher.scaleToPixels(source, tilesX * MapArtService.TILE, tilesY * MapArtService.TILE);
        return MapArtService.quantizeTiles(pixels, tilesX, tilesY, dither);
    }
}
