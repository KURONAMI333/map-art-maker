package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.config.ModConfig;
import com.kuronami.mapartmaker.mapart.MapArtService;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

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
        /** The dimension the transfer started in; see {@link UpdateResult.Complete#dimension()}. */
        final ResourceKey<Level> dimension;
        final TileTransferState state;
        volatile long lastUpdatedAt;

        Entry(BlockPos pos, ResourceKey<Level> dimension, TileTransferState state, long now) {
            this.pos = pos;
            this.dimension = dimension;
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

        /**
         * Every tile arrived; {@code tiles} is reading order, ready to store.
         *
         * @param dimension the dimension recorded when this transfer's first tile arrived, not
         *                   necessarily where the player is now
         */
        record Complete(int tilesX, int tilesY, byte[][] tiles, ResourceKey<Level> dimension) implements UpdateResult {
        }
    }

    /** How an incoming {@code (transferId, pos, size)} triple relates to whatever is in flight. */
    private enum Identity {
        /** Continues the entry already keyed under this player. */
        CONTINUE,
        /** Same {@code transferId}/{@code pos} as the entry in flight, but a different size. */
        SIZE_MISMATCH,
        /** No entry, or a genuinely different transfer (new id and/or a different block). */
        NEW
    }

    private static Identity identify(Entry entry, int transferId, BlockPos pos, int size) {
        if (entry == null || entry.state.transferId() != transferId || !entry.pos.equals(pos)) {
            return Identity.NEW;
        }
        return entry.state.tilesX() == size ? Identity.CONTINUE : Identity.SIZE_MISMATCH;
    }

    /**
     * True when this packet would start a fresh transfer. Lets {@link #handleTile} gate the doomed
     * case before spending a full 9-packet transfer on it; see {@link #update} for the matching
     * runtime behaviour on the other two {@link Identity} outcomes.
     */
    static boolean isNewTransfer(UUID playerId, BlockPos pos, int transferId, int size) {
        return identify(TRANSFERS.get(playerId), transferId, pos, size) == Identity.NEW;
    }

    /**
     * Pure bookkeeping: no world or player types (only value-ish identifiers, same as
     * {@code pos}), so this is unit testable directly. Reach, the pre-transfer stock gate and
     * feedback delivery live in {@link #handleTile}, which wraps this.
     */
    public static UpdateResult update(UUID playerId, BlockPos pos, ResourceKey<Level> dimension, int transferId,
                                       int size, int tileIndex, byte[] colours) {
        if (!sizeAllowed(size)) {
            discard(playerId);
            return new UpdateResult.Rejected("message.map_art_maker.bad_size");
        }

        long now = System.currentTimeMillis();
        Entry entry = TRANSFERS.get(playerId);
        Identity identity = identify(entry, transferId, pos, size);
        switch (identity) {
            case SIZE_MISMATCH -> {
                // Same transfer, same block, but a different size than it started with: something
                // is wrong on the client side. Reject outright rather than silently keep the first
                // size (the identity check used to ignore size entirely) or silently start over
                // (which would mix two grids' tiles).
                discard(playerId);
                return new UpdateResult.Rejected("message.map_art_maker.upload_failed");
            }
            case NEW -> {
                entry = new Entry(pos, dimension, new TileTransferState(transferId, size, size), now);
                TRANSFERS.put(playerId, entry);
            }
            case CONTINUE -> {
                // entry already refers to the right transfer.
            }
        }

        try {
            boolean complete = entry.state.admit(tileIndex, colours);
            entry.lastUpdatedAt = now;
            if (complete) {
                TRANSFERS.remove(playerId);
                return new UpdateResult.Complete(entry.state.tilesX(), entry.state.tilesY(), entry.state.tilesInOrder(),
                        entry.dimension);
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

        // Before committing to a 9-packet transfer: if it is doomed already (no block, or not
        // enough blanks for the size being offered), say so on the first packet instead of making
        // the client upload the whole grid for nothing. Only checked when this packet would start a
        // new transfer — everything a continuing transfer needs was already checked here.
        if (isNewTransfer(player.getUUID(), payload.pos(), payload.transferId(), payload.size())
                && rejectDoomedTransfer(player, payload.pos(), payload.size())) {
            return;
        }

        UpdateResult result = update(player.getUUID(), payload.pos(), player.serverLevel().dimension(),
                payload.transferId(), payload.size(), payload.tileIndex(), payload.colours());
        switch (result) {
            case UpdateResult.Rejected rejected -> ModNetwork.fail(player, rejected.reasonKey());
            case UpdateResult.Pending ignored -> {
                // more tiles expected, nothing to do yet
            }
            case UpdateResult.Complete complete -> assemble(player, payload.pos(), complete);
        }
    }

    /**
     * @return true if the transfer was rejected and the caller should stop; false if it may proceed
     */
    private static boolean rejectDoomedTransfer(ServerPlayer player, BlockPos pos, int size) {
        BlockEntity be = player.serverLevel().getBlockEntity(pos);
        if (!(be instanceof MapArtMakerBlockEntity maker)) {
            discard(player.getUUID());
            ModNetwork.fail(player, "message.map_art_maker.no_block");
            return true;
        }
        int required = size * size;
        if (maker.blankCount() < required) {
            discard(player.getUUID());
            ModNetwork.fail(player, "message.map_art_maker.need_maps", required);
            return true;
        }
        return false;
    }

    /**
     * Back on the server thread already (payload handlers run there): hands off to
     * {@link ModNetwork#storeAssembledMaps}, which re-checks everything (including the dimension
     * this transfer started in) and only then builds the maps.
     */
    private static void assemble(ServerPlayer player, BlockPos pos, UpdateResult.Complete complete) {
        ModNetwork.storeAssembledMaps(player, pos, complete.dimension(), complete.tilesX(), complete.tilesY(),
                level -> MapArtService.createMaps(level, complete.tiles()));
    }
}
