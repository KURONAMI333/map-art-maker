package com.kuronami.mapartmaker.mapart;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The drop path must land on exactly what the URL path would produce for the same image: both
 * funnel through {@link ImageFetcher#scaleToPixels} and {@link MapArtService#quantizeTiles}, so
 * the parity is structural rather than merely asserted here — this test is about tile ordering
 * and the drop-specific failure cases (oversized, unreadable) that the URL path never exercises.
 */
class LocalMapArtEncoderTest {

    private static byte[] pngOf(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static BufferedImage solid(int width, int height, Color colour) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(colour);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    @Test
    void encodesTheDeclaredNumberOfTilesInReadingOrder() throws Exception {
        byte[] png = pngOf(solid(64, 64, new Color(0x20, 0x80, 0xC0)));

        byte[][] tiles = LocalMapArtEncoder.encode(png, 2, 2, false);

        assertEquals(4, tiles.length);
        int tileBytes = MapArtService.TILE * MapArtService.TILE;
        for (byte[] tile : tiles) {
            assertEquals(tileBytes, tile.length);
        }
    }

    @Test
    void matchesTheServerPathForTheIdenticalPixels() throws Exception {
        BufferedImage source = solid(50, 50, new Color(0x11, 0x22, 0x33));
        byte[] png = pngOf(source);

        byte[][] fromDrop = LocalMapArtEncoder.encode(png, 1, 1, true);

        int[] pixels = ImageFetcher.scaleToPixels(source, MapArtService.TILE, MapArtService.TILE);
        byte[][] fromUrlStyle = MapArtService.quantizeTiles(pixels, 1, 1, true);

        assertArrayEquals(fromUrlStyle[0], fromDrop[0]);
    }

    @Test
    void rejectsBytesLargerThanTheCap() {
        byte[] tooBig = new byte[ImageFetcher.MAX_BYTES + 1];

        LocalMapArtEncoder.LocalImageException e = assertThrows(LocalMapArtEncoder.LocalImageException.class,
                () -> LocalMapArtEncoder.encode(tooBig, 1, 1, false));
        assertEquals("message.map_art_maker.drop_too_large", e.translationKey());
    }

    @Test
    void rejectsBytesThatAreNotAnImage() {
        byte[] garbage = "definitely not a png".getBytes();

        LocalMapArtEncoder.LocalImageException e = assertThrows(LocalMapArtEncoder.LocalImageException.class,
                () -> LocalMapArtEncoder.encode(garbage, 1, 1, false));
        assertEquals("message.map_art_maker.drop_not_image", e.translationKey());
    }
}
