package com.kuronami.mapartmaker.client;

import com.kuronami.mapartmaker.mapart.ImageFetcher;
import com.kuronami.mapartmaker.mapart.LocalMapArtEncoder;
import com.kuronami.mapartmaker.network.MapArtTilePayload;
import com.kuronami.mapartmaker.platform.Services;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Turns a file dropped on the GUI into {@link MapArtTilePayload} packets.
 *
 * <p>Reading, decoding, scaling and quantising all happen off the render thread — the same
 * reasoning as {@code ModNetwork}'s download pool, just client side. Sending the tiles hops back
 * onto the client thread, since {@code Services.NETWORK} is not documented as safe to call from
 * an arbitrary background thread.
 */
public final class MapArtDropHandler {

    private static final Executor ENCODE = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "map-art-maker-drop-encode");
        thread.setDaemon(true);
        return thread;
    });

    /** Distinguishes this drop's tiles from a previous, possibly still in-flight, one. */
    private static final AtomicInteger NEXT_TRANSFER_ID = new AtomicInteger();

    private MapArtDropHandler() {
    }

    public static void handleDrop(BlockPos pos, Path path, int tiles, boolean dither) {
        CompletableFuture
                .supplyAsync(() -> readAndEncode(path, tiles, dither), ENCODE)
                .whenComplete((tileBytes, error) -> Minecraft.getInstance().execute(
                        () -> finish(pos, tiles, dither, tileBytes, error)));
    }

    private static byte[][] readAndEncode(Path path, int tiles, boolean dither) {
        byte[] fileBytes;
        try {
            if (Files.size(path) > ImageFetcher.MAX_BYTES) {
                throw new StageFailure("message.map_art_maker.drop_too_large");
            }
            fileBytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new StageFailure("message.map_art_maker.drop_unreadable");
        }

        try {
            return LocalMapArtEncoder.encode(fileBytes, tiles, tiles, dither);
        } catch (LocalMapArtEncoder.LocalImageException e) {
            throw new StageFailure(e.translationKey());
        }
    }

    private static void finish(BlockPos pos, int tiles, boolean dither, byte[][] tileBytes, Throwable error) {
        if (error != null) {
            Throwable cause = error instanceof CompletionException ? error.getCause() : error;
            String key = cause instanceof StageFailure failure
                    ? failure.getMessage()
                    : "message.map_art_maker.drop_unreadable";
            MapArtFeedbackHolder.accept(false, Component.translatable(key));
            return;
        }

        int transferId = NEXT_TRANSFER_ID.incrementAndGet();
        for (int i = 0; i < tileBytes.length; i++) {
            Services.NETWORK.sendToServer(new MapArtTilePayload(pos, transferId, tiles, i, dither, tileBytes[i]));
        }
    }

    /** Carries a translation key out of the encode thread. */
    private static final class StageFailure extends RuntimeException {
        StageFailure(String translationKey) {
            super(translationKey, null, false, false);
        }
    }
}
