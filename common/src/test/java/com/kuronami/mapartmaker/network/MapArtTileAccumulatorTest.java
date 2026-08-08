package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.config.ModConfig;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Exercises {@link MapArtTileAccumulator#update} and its lifecycle hooks directly — no
 * {@code ServerPlayer} or {@code ServerLevel} involved, so this needs no game bootstrap. Every
 * test uses its own random player id so the accumulator's shared static map cannot leak state
 * between tests.
 */
class MapArtTileAccumulatorTest {

    private static final BlockPos POS = new BlockPos(1, 2, 3);

    @AfterEach
    void restoreDefaultConfig() {
        ModConfig.apply(ModConfig.DEFAULT_MAX_TILES_PER_SIDE, ModConfig.DEFAULT_ALLOW_PRIVATE_HOSTS);
    }

    private static byte[] tile(int fill) {
        byte[] out = new byte[MapArtTilePayload.TILE_BYTES];
        java.util.Arrays.fill(out, (byte) fill);
        return out;
    }

    @Test
    void aSingleTileTransferCompletesImmediately() {
        UUID player = UUID.randomUUID();
        var result = MapArtTileAccumulator.update(player, POS, 1, 1, 0, false, tile(5));

        var complete = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Complete.class, result);
        assertEquals(1, complete.tilesX());
        assertEquals(1, complete.tilesY());
        assertArrayEquals(tile(5), complete.tiles()[0]);
    }

    @Test
    void completingATransferReturnsAllTilesInReadingOrder() {
        UUID player = UUID.randomUUID();
        byte[][] tiles = {tile(11), tile(22), tile(33), tile(44)};

        MapArtTileAccumulator.UpdateResult result = null;
        for (int i = 0; i < tiles.length; i++) {
            result = MapArtTileAccumulator.update(player, POS, 3, 2, i, false, tiles[i]);
        }

        var complete = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Complete.class, result);
        assertEquals(2, complete.tilesX());
        assertEquals(2, complete.tilesY());
        for (int i = 0; i < tiles.length; i++) {
            assertArrayEquals(tiles[i], complete.tiles()[i], "tile " + i);
        }
    }

    @Test
    void declaringMoreTilesThanTheConfigCeilingIsRejected() {
        UUID player = UUID.randomUUID();
        // Default ceiling is 3.
        var result = MapArtTileAccumulator.update(player, POS, 1, 4, 0, false, tile(1));

        var rejected = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, result);
        assertEquals("message.map_art_maker.bad_size", rejected.reasonKey());
    }

    @Test
    void aBadSizeDeclarationDiscardsWhateverWasPending() {
        UUID player = UUID.randomUUID();
        var first = MapArtTileAccumulator.update(player, POS, 1, 2, 0, false, tile(1));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, first);

        var rejected = MapArtTileAccumulator.update(player, POS, 1, 0, 1, false, tile(2));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, rejected);

        // Same transfer id, same tile 0: if the reject above had not discarded the pending state,
        // this would be treated as a duplicate of the tile admitted in `first`.
        var resumed = MapArtTileAccumulator.update(player, POS, 1, 2, 0, false, tile(3));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, resumed);
    }

    @Test
    void anOutOfRangeTileIndexIsRejectedAndDiscards() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, 5, 2, 0, false, tile(1));

        var rejected = MapArtTileAccumulator.update(player, POS, 5, 2, 4, false, tile(2));
        var r = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, rejected);
        assertEquals("message.map_art_maker.upload_failed", r.reasonKey());

        var resumed = MapArtTileAccumulator.update(player, POS, 5, 2, 0, false, tile(3));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, resumed,
                "the reject should have discarded the transfer rather than leave it half-admitted");
    }

    @Test
    void aDuplicateTileIndexIsRejectedAndDiscards() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, 6, 2, 0, false, tile(1));

        var rejected = MapArtTileAccumulator.update(player, POS, 6, 2, 0, false, tile(2));
        var r = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, rejected);
        assertEquals("message.map_art_maker.upload_failed", r.reasonKey());

        var resumed = MapArtTileAccumulator.update(player, POS, 6, 2, 0, false, tile(3));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, resumed);
    }

    @Test
    void aBadColourLengthIsRejectedAndDiscards() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, 9, 2, 0, false, tile(1));

        var rejected = MapArtTileAccumulator.update(player, POS, 9, 2, 1, false, new byte[10]);
        var r = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, rejected);
        assertEquals("message.map_art_maker.upload_failed", r.reasonKey());
    }

    @Test
    void aFreshTransferIdSilentlyReplacesAnAbandonedOne() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, 20, 2, 0, false, tile(1));

        // Different transfer id: the old, incomplete state is dropped rather than mixed in.
        var result = MapArtTileAccumulator.update(player, POS, 21, 1, 0, false, tile(2));
        var complete = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Complete.class, result);
        assertArrayEquals(tile(2), complete.tiles()[0]);
    }

    @Test
    void discardRemovesThePendingTransfer() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, 30, 2, 0, false, tile(1));

        MapArtTileAccumulator.discard(player);

        // If discard had not run, this would be a duplicate of the tile admitted above.
        var result = MapArtTileAccumulator.update(player, POS, 30, 2, 0, false, tile(2));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, result);
    }

    @Test
    void sweepEvictsAnEntryThatWentQuietPastTheTimeout() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, 40, 2, 0, false, tile(1));

        MapArtTileAccumulator.sweepExpired(System.currentTimeMillis() + 100_000L);

        var result = MapArtTileAccumulator.update(player, POS, 40, 2, 0, false, tile(2));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, result,
                "the sweep should have discarded the stale transfer rather than leave it in place");
    }

    @Test
    void sweepLeavesARecentTransferAlone() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, 41, 2, 0, false, tile(1));

        MapArtTileAccumulator.sweepExpired(System.currentTimeMillis());

        var result = MapArtTileAccumulator.update(player, POS, 41, 2, 0, false, tile(2));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, result,
                "a transfer that has not gone quiet must not be swept");
    }
}
