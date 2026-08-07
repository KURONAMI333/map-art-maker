package com.kuronami.mapartmaker.mapart;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Downloads an image and scales it to a map-art canvas.
 *
 * <p>Runs on the server with a URL a player typed, so the guards here are load-bearing:
 * scheme allow-list, private-address rejection, byte cap and timeout.
 */
public final class ImageFetcher {

    public static final int MAX_BYTES = 8 * 1024 * 1024;
    public static final Duration TIMEOUT = Duration.ofSeconds(10);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private ImageFetcher() {
    }

    /** Thrown for every rejection a player could cause, so callers can surface the text verbatim. */
    public static class FetchException extends Exception {
        public FetchException(String message) {
            super(message);
        }
    }

    /**
     * @param allowPrivateHosts set from config when an operator deliberately serves images from the LAN
     * @return ARGB pixels, row-major, exactly {@code tilesX*128} by {@code tilesY*128}
     */
    public static int[] fetchScaled(String url, int tilesX, int tilesY, boolean allowPrivateHosts)
            throws FetchException {
        URI uri = parse(url);
        guardHost(uri, allowPrivateHosts);

        byte[] body = download(uri);
        BufferedImage source = decode(body);
        return scaleToPixels(source, tilesX * MapArtService.TILE, tilesY * MapArtService.TILE);
    }

    private static URI parse(String url) throws FetchException {
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            throw new FetchException("that does not look like a URL");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new FetchException("only http and https links are supported");
        }
        if (uri.getHost() == null) {
            throw new FetchException("that URL has no host");
        }
        return uri;
    }

    private static void guardHost(URI uri, boolean allowPrivateHosts) throws FetchException {
        if (allowPrivateHosts) {
            return;
        }
        InetAddress address;
        try {
            address = InetAddress.getByName(uri.getHost());
        } catch (UnknownHostException e) {
            throw new FetchException("could not resolve " + uri.getHost());
        }
        if (address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress()
                || address.isAnyLocalAddress() || address.isMulticastAddress()) {
            throw new FetchException("links to private addresses are blocked");
        }
    }

    private static byte[] download(URI uri) throws FetchException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("User-Agent", "MapArtMaker")
                .GET()
                .build();
        try {
            HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                throw new FetchException("the server answered " + response.statusCode());
            }
            try (InputStream in = response.body()) {
                return readCapped(in);
            }
        } catch (IOException e) {
            throw new FetchException("could not download that image");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FetchException("the download was interrupted");
        }
    }

    /** Reads at most {@link #MAX_BYTES} so a huge file cannot exhaust the server heap. */
    private static byte[] readCapped(InputStream in) throws IOException, FetchException {
        byte[] buffer = new byte[8192];
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > MAX_BYTES) {
                throw new FetchException("that image is larger than " + (MAX_BYTES / (1024 * 1024)) + " MB");
            }
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static BufferedImage decode(byte[] body) throws FetchException {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(body));
            if (image == null) {
                throw new FetchException("that link is not an image file");
            }
            return image;
        } catch (IOException e) {
            throw new FetchException("that image could not be read");
        }
    }

    /** Bilinear downscale into an ARGB array. Package-private so tests can drive it without the network. */
    static int[] scaleToPixels(BufferedImage source, int width, int height) {
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = canvas.createGraphics();
        try {
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
        } finally {
            g.dispose();
        }
        int[] pixels = new int[width * height];
        canvas.getRGB(0, 0, width, height, pixels, 0, width);
        return pixels;
    }
}
