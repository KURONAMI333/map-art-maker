package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.config.ModConfig;
import com.kuronami.mapartmaker.mapart.ImageFetcher;
import com.kuronami.mapartmaker.mapart.MapArtService;
import com.kuronami.mapartmaker.platform.Services;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;

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
        // Recorded now, at the request, and re-checked once the pixels are back: the download can
        // take real time, and nothing stops the player stepping through a portal while it runs.
        ResourceKey<Level> dimension = level.dimension();

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
                        () -> finish(player, payload, dimension, pixels, error)));
    }

    /** Back on the server thread: world state may only be touched here. */
    private static void finish(ServerPlayer player, CreateMapArtPayload payload, ResourceKey<Level> dimension,
                                int[] pixels, Throwable error) {
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

        int tilesX = payload.tilesX();
        int tilesY = payload.tilesY();
        boolean dither = payload.dither();
        storeAssembledMaps(player, payload.pos(), dimension, tilesX, tilesY,
                level -> MapArtService.createTiles(level, pixels, tilesX, tilesY, dither));
    }

    /**
     * Re-checks stock and, only once every check has passed, builds and stores the tiles.
     *
     * <p>Shared by the URL path (above, after a download) and {@link MapArtTileAccumulator} (after
     * a file-drop upload finishes reassembling): either way real time passed since the request
     * started, so the container — and the player's dimension — may have changed. {@code mapSupplier}
     * is deferred rather than a plain {@code List<ItemStack>} on purpose: minting a map id is
     * permanent world state ({@link ServerLevel#getFreeMapId()} advances a saved counter,
     * {@link ServerLevel#setMapData} writes a {@code map_N.dat} entry), so nothing may be minted
     * until every rejection path below has had its say.
     *
     * <p>Public because the GameTests that guard those rejection paths live in a separate,
     * development-only mod ({@code neoforge/src/gametest}), and FML loads each mod as its own
     * JPMS module — a test class cannot share this package.
     *
     * @param dimension the dimension the request was made in; storing is refused if the player is
     *                   no longer there, since {@code pos} would otherwise resolve to whatever
     *                   block happens to sit at those coordinates in the new dimension
     */
    public static void storeAssembledMaps(ServerPlayer player, BlockPos pos, ResourceKey<Level> dimension,
                                    int tilesX, int tilesY, Function<ServerLevel, List<ItemStack>> mapSupplier) {
        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(dimension)) {
            fail(player, "message.map_art_maker.no_block");
            return;
        }

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
        if (!maker.canPlace(tilesX, tilesY)) {
            fail(player, "message.map_art_maker.no_room");
            return;
        }

        List<ItemStack> maps = mapSupplier.apply(level);
        if (!maker.consumeBlanksAndStore(tilesX, tilesY, maps)) {
            // Nothing else runs on the server thread between the canPlace check above and here, so
            // this is unreachable in practice; kept as a hard backstop rather than trusting that
            // invariant to hold forever.
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
