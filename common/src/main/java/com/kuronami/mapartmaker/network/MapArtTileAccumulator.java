package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.config.ModConfig;
import com.kuronami.mapartmaker.mapart.MapArtService;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side reassembly of the file-drop upload ({@link MapArtTilePayload}: one packet per tile).
 *
 * <p>One in-flight transfer per player. A fresh {@code transferId} (or a different target block)
 * silently replaces whatever that player had in flight — the old bytes are simply dropped for GC,
 * which is the same outcome as an explicit discard. Three things guarantee nothing outlives its
 * player: {@link #discard} is called when the GUI closes ({@code MapArtMakerMenu.removed}) and on
 * disconnect (each loader's own hook), and {@link #sweepExpired()} is a cheap per-tick check that
 * catches anything neither of those reached (alt-F4, a lost final packet, ...).
 */
public final class MapArtTileAccumulator {

    /** No progress for this long and a transfer is treated as abandoned. */
    private static final long TIMEOUT_MILLIS = 30_000L;

    private static final Map<UUID, Entry> TRANSFERS = new ConcurrentHashMap<>();

    private MapArtTileAccumulator() {
    }

    private static final class Entry {
        final BlockPos pos;
        final TileTransferState state;
        volatile long lastUpdatedAt;

        Entry(BlockPos pos, TileTransferState state, long now) {
            this.pos = pos;
            this.state = state;
            this.lastUpdatedAt = now;
        }
    }

    /** What one incoming tile packet did to the accumulator. */
    public sealed interface UpdateResult {
        /** @param reasonKey a {@code message.map_art_maker.*} translation key */
        record Rejected(String reasonKey) implements UpdateResult {
        }

        /** More tiles are still expected. */
        record Pending() implements UpdateResult {
        }

        /** Every tile arrived; {@code tiles} is reading order, ready to store. */
        record Complete(int tilesX, int tilesY, byte[][] tiles) implements UpdateResult {
        }
    }

    /**
     * Pure bookkeeping: no world or player types, so this is unit testable directly. Reach and
     * feedback delivery live in {@link #handleTile}, which wraps this.
     */
    public static UpdateResult update(UUID playerId, BlockPos pos, int transferId, int size, int tileIndex,
                                       boolean dither, byte[] colours) {
        if (!sizeAllowed(size)) {
            discard(playerId);
            return new UpdateResult.Rejected("message.map_art_maker.bad_size");
        }

        long now = System.currentTimeMillis();
        Entry entry = TRANSFERS.get(playerId);
        if (entry == null || entry.state.transferId() != transferId || !entry.pos.equals(pos)) {
            entry = new Entry(pos, new TileTransferState(transferId, size, size, dither), now);
            TRANSFERS.put(playerId, entry);
        }

        try {
            boolean complete = entry.state.admit(tileIndex, colours);
            entry.lastUpdatedAt = now;
            if (complete) {
                TRANSFERS.remove(playerId);
                return new UpdateResult.Complete(entry.state.tilesX(), entry.state.tilesY(), entry.state.tilesInOrder());
            }
            return new UpdateResult.Pending();
        } catch (IllegalArgumentException | IllegalStateException e) {
            TRANSFERS.remove(playerId);
            return new UpdateResult.Rejected("message.map_art_maker.upload_failed");
        }
    }

    static boolean sizeAllowed(int size) {
        return size >= 1 && size <= ModConfig.maxTilesPerSide();
    }

    /** Called when the GUI closes, on disconnect, or after a reject — never lets state linger. */
    public static void discard(UUID playerId) {
        TRANSFERS.remove(playerId);
    }

    /** Called every server tick; a no-op unless something is actually in flight. */
    public static void sweepExpired() {
        sweepExpired(System.currentTimeMillis());
    }

    static void sweepExpired(long nowMillis) {
        if (TRANSFERS.isEmpty()) {
            return;
        }
        TRANSFERS.values().removeIf(e -> nowMillis - e.lastUpdatedAt > TIMEOUT_MILLIS);
    }

    /** Called on the server thread by each loader's receive hook. */
    public static void handleTile(MapArtTilePayload payload, ServerPlayer player) {
        sweepExpired();

        if (player.distanceToSqr(payload.pos().getCenter()) > ModNetwork.REACH_SQUARED) {
            discard(player.getUUID());
            ModNetwork.fail(player, "message.map_art_maker.too_far");
            return;
        }

        UpdateResult result = update(player.getUUID(), payload.pos(), payload.transferId(), payload.size(),
                payload.tileIndex(), payload.dither(), payload.colours());
        switch (result) {
            case UpdateResult.Rejected rejected -> ModNetwork.fail(player, rejected.reasonKey());
            case UpdateResult.Pending ignored -> {
                // more tiles expected, nothing to do yet
            }
            case UpdateResult.Complete complete -> assemble(player, payload.pos(), complete);
        }
    }

    /**
     * Back on the server thread already (payload handlers run there): builds the maps and hands
     * off to {@link ModNetwork#storeAssembledMaps}, which does its own block-entity and stock check.
     */
    private static void assemble(ServerPlayer player, BlockPos pos, UpdateResult.Complete complete) {
        ServerLevel level = player.serverLevel();
        List<ItemStack> maps = new ArrayList<>(complete.tiles().length);
        for (byte[] colours : complete.tiles()) {
            maps.add(MapArtService.createMap(level, colours));
        }
        ModNetwork.storeAssembledMaps(player, pos, complete.tilesX(), complete.tilesY(), maps);
    }
}
