package com.kuronami.mapartmaker.mapart;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageFetcherTest {

    @Test
    void scalingProducesExactCanvasSize() {
        BufferedImage source = new BufferedImage(500, 300, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ImageFetcher.scaleToPixels(source, 256, 128);
        assertEquals(256 * 128, pixels.length);
    }

    @Test
    void scalingKeepsASolidColour() {
        BufferedImage source = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = source.createGraphics();
        g.setColor(new java.awt.Color(0x20, 0x80, 0xC0));
        g.fillRect(0, 0, 64, 64);
        g.dispose();

        int[] pixels = ImageFetcher.scaleToPixels(source, 128, 128);
        // Interior pixels must survive resampling unchanged; edges can pick up alpha from the canvas.
        int centre = pixels[64 * 128 + 64];
        assertEquals(0x20, (centre >> 16) & 0xFF);
        assertEquals(0x80, (centre >> 8) & 0xFF);
        assertEquals(0xC0, centre & 0xFF);
        assertEquals(0xFF, (centre >>> 24) & 0xFF);
    }

    @Test
    void rejectsNonHttpSchemes() {
        assertRejected("file:///etc/passwd", "only http and https");
        assertRejected("ftp://example.com/a.png", "only http and https");
        assertRejected("https:///no-host.png", "no host");
    }

    @Test
    void rejectsMalformedInput() {
        assertRejected("not a url at all", "does not look like a URL");
    }

    @Test
    void rejectsPrivateAndLoopbackHosts() {
        assertRejected("http://127.0.0.1/a.png", "private addresses");
        assertRejected("http://192.168.1.1/a.png", "private addresses");
        assertRejected("http://10.0.0.5/a.png", "private addresses");
        assertRejected("http://169.254.169.254/latest/meta-data", "private addresses");
    }

    private static void assertRejected(String url, String expectedFragment) {
        ImageFetcher.FetchException e = assertThrows(ImageFetcher.FetchException.class,
                () -> ImageFetcher.fetchScaled(url, 1, 1, false));
        assertTrue(e.getMessage().contains(expectedFragment),
                "expected message about '" + expectedFragment + "' but got: " + e.getMessage());
    }
}
