package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.config.ModConfig;
import com.kuronami.mapartmaker.mapart.ImageFetcher;
import com.kuronami.mapartmaker.mapart.MapArtService;
import com.kuronami.mapartmaker.platform.Services;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Loader-independent packet handling. Each loader registers the payload types and routes
 * incoming packets here.
 */
public final class ModNetwork {

    /** How far a player may stand from the block and still drive it. */
    static final double REACH_SQUARED = 64.0D;

    /** Downloads must never run on the server thread. One shared, daemon, bounded pool. */
    private static final Executor DOWNLOADS = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "map-art-maker-download");
        thread.setDaemon(true);
        return thread;
    });

    private ModNetwork() {
    }

    /** Called on the server thread by each loader's receive hook. */
    public static void handleCreateMapArt(CreateMapArtPayload payload, ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        if (payload.tilesX() < 1 || payload.tilesY() < 1
                || payload.tilesX() > ModConfig.maxTilesPerSide()
                || payload.tilesY() > ModConfig.maxTilesPerSide()) {
            fail(player, "message.map_art_maker.bad_size");
            return;
        }
        if (player.distanceToSqr(payload.pos().getCenter()) > REACH_SQUARED) {
            fail(player, "message.map_art_maker.too_far");
            return;
        }

        BlockEntity be = level.getBlockEntity(payload.pos());
        if (!(be instanceof MapArtMakerBlockEntity maker)) {
            fail(player, "message.map_art_maker.no_block");
            return;
        }

        int required = payload.tilesX() * payload.tilesY();
        if (maker.blankCount() < required) {
            fail(player, "message.map_art_maker.need_maps", required);
            return;
        }
        if (maker.freeOutputSlots() < required) {
            fail(player, "message.map_art_maker.no_room");
            return;
        }

        String url = payload.url();
        int tilesX = payload.tilesX();
        int tilesY = payload.tilesY();
        boolean dither = payload.dither();

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return ImageFetcher.fetchScaled(url, tilesX, tilesY, ModConfig.allowPrivateHosts());
                    } catch (ImageFetcher.FetchException e) {
                        throw new CompletionFailure(e.getMessage());
                    }
                }, DOWNLOADS)
                .whenComplete((pixels, error) -> level.getServer().execute(
                        () -> finish(player, payload, pixels, error)));
    }

    /** Back on the server thread: world state may only be touched here. */
    private static void finish(ServerPlayer player, CreateMapArtPayload payload, int[] pixels, Throwable error) {
        if (error != null) {
            Throwable cause = error instanceof java.util.concurrent.CompletionException ? error.getCause() : error;
            if (cause instanceof CompletionFailure failure) {
                send(player, false, Component.literal(failure.getMessage()));
            } else {
                Constants.LOG.warn("map art generation failed", cause);
                fail(player, "message.map_art_maker.failed");
            }
            return;
        }

        List<ItemStack> maps = MapArtService.createTiles(player.serverLevel(), pixels, payload.tilesX(), payload.tilesY(),
                payload.dither());
        storeAssembledMaps(player, payload.pos(), payload.tilesX(), payload.tilesY(), maps);
    }

    /**
     * Re-checks stock and stores already-built tiles. Shared by the URL path (above, after a
     * download) and {@link MapArtTileAccumulator} (after a file-drop upload finishes reassembling):
     * either way the pixels took real time to arrive, so the container may have changed since the
     * request started.
     */
    static void storeAssembledMaps(ServerPlayer player, BlockPos pos, int tilesX, int tilesY, List<ItemStack> maps) {
        ServerLevel level = player.serverLevel();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MapArtMakerBlockEntity maker)) {
            fail(player, "message.map_art_maker.no_block");
            return;
        }

        int required = tilesX * tilesY;
        if (maker.blankCount() < required) {
            fail(player, "message.map_art_maker.need_maps", required);
            return;
        }
        if (!maker.consumeBlanksAndStore(tilesX, tilesY, maps)) {
            fail(player, "message.map_art_maker.no_room");
            return;
        }
        send(player, true, Component.translatable("message.map_art_maker.done", required));
    }

    static void fail(ServerPlayer player, String key, Object... args) {
        send(player, false, Component.translatable(key, args));
    }

    static void send(ServerPlayer player, boolean success, Component message) {
        Services.NETWORK.sendToPlayer(player, new MapArtFeedbackPayload(success, message));
    }

    /** Carries a player-facing reason out of the download thread. */
    private static final class CompletionFailure extends RuntimeException {
        private CompletionFailure(String message) {
            super(message, null, false, false);
        }
    }
}
