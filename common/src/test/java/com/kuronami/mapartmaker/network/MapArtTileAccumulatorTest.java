package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.config.ModConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

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
 * between tests. {@link Level#OVERWORLD}/{@link Level#NETHER} are plain static
 * {@code ResourceKey} values and, like {@link BlockPos}, need no game bootstrap either.
 */
class MapArtTileAccumulatorTest {

    private static final BlockPos POS = new BlockPos(1, 2, 3);
    private static final ResourceKey<Level> OVERWORLD = Level.OVERWORLD;
    private static final ResourceKey<Level> NETHER = Level.NETHER;

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
        var result = MapArtTileAccumulator.update(player, POS, OVERWORLD, 1, 1, 0, tile(5));

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
            result = MapArtTileAccumulator.update(player, POS, OVERWORLD, 3, 2, i, tiles[i]);
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
        var result = MapArtTileAccumulator.update(player, POS, OVERWORLD, 1, 4, 0, tile(1));

        var rejected = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, result);
        assertEquals("message.map_art_maker.bad_size", rejected.reasonKey());
    }

    @Test
    void aBadSizeDeclarationDiscardsWhateverWasPending() {
        UUID player = UUID.randomUUID();
        var first = MapArtTileAccumulator.update(player, POS, OVERWORLD, 1, 2, 0, tile(1));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, first);

        var rejected = MapArtTileAccumulator.update(player, POS, OVERWORLD, 1, 0, 1, tile(2));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, rejected);

        // Same transfer id, same tile 0: if the reject above had not discarded the pending state,
        // this would be treated as a duplicate of the tile admitted in `first`.
        var resumed = MapArtTileAccumulator.update(player, POS, OVERWORLD, 1, 2, 0, tile(3));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, resumed);
    }

    @Test
    void anOutOfRangeTileIndexIsRejectedAndDiscards() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, OVERWORLD, 5, 2, 0, tile(1));

        var rejected = MapArtTileAccumulator.update(player, POS, OVERWORLD, 5, 2, 4, tile(2));
        var r = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, rejected);
        assertEquals("message.map_art_maker.upload_failed", r.reasonKey());

        var resumed = MapArtTileAccumulator.update(player, POS, OVERWORLD, 5, 2, 0, tile(3));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, resumed,
                "the reject should have discarded the transfer rather than leave it half-admitted");
    }

    @Test
    void aDuplicateTileIndexIsRejectedAndDiscards() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, OVERWORLD, 6, 2, 0, tile(1));

        var rejected = MapArtTileAccumulator.update(player, POS, OVERWORLD, 6, 2, 0, tile(2));
        var r = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, rejected);
        assertEquals("message.map_art_maker.upload_failed", r.reasonKey());

        var resumed = MapArtTileAccumulator.update(player, POS, OVERWORLD, 6, 2, 0, tile(3));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, resumed);
    }

    @Test
    void aBadColourLengthIsRejectedAndDiscards() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, OVERWORLD, 9, 2, 0, tile(1));

        var rejected = MapArtTileAccumulator.update(player, POS, OVERWORLD, 9, 2, 1, new byte[10]);
        var r = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, rejected);
        assertEquals("message.map_art_maker.upload_failed", r.reasonKey());
    }

    @Test
    void aFreshTransferIdSilentlyReplacesAnAbandonedOne() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, OVERWORLD, 20, 2, 0, tile(1));

        // Different transfer id: the old, incomplete state is dropped rather than mixed in.
        var result = MapArtTileAccumulator.update(player, POS, OVERWORLD, 21, 1, 0, tile(2));
        var complete = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Complete.class, result);
        assertArrayEquals(tile(2), complete.tiles()[0]);
    }

    @Test
    void aMidTransferSizeChangeIsRejectedAndDiscardsRatherThanSilentlyIgnored() {
        UUID player = UUID.randomUUID();
        // Same transfer id and block, but the second packet claims a different grid size.
        var first = MapArtTileAccumulator.update(player, POS, OVERWORLD, 50, 2, 0, tile(1));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, first);

        var rejected = MapArtTileAccumulator.update(player, POS, OVERWORLD, 50, 3, 1, tile(2));
        var r = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, rejected);
        assertEquals("message.map_art_maker.upload_failed", r.reasonKey());

        // The mismatch must have discarded the transfer rather than leaving the 2x2 half-admitted.
        var resumed = MapArtTileAccumulator.update(player, POS, OVERWORLD, 50, 2, 0, tile(3));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, resumed);
    }

    @Test
    void theDimensionRecordedAtTheFirstTileSurvivesToCompletion() {
        UUID player = UUID.randomUUID();
        // First tile (of a 2x2 = 4 tile grid) starts the transfer while, as far as the caller is
        // concerned, the player is in the overworld.
        var pending = MapArtTileAccumulator.update(player, POS, OVERWORLD, 62, 2, 0, tile(3));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, pending);
        MapArtTileAccumulator.update(player, POS, OVERWORLD, 62, 2, 1, tile(4));
        MapArtTileAccumulator.update(player, POS, OVERWORLD, 62, 2, 2, tile(5));

        // Final tile is reported from the nether: a real caller would pass whatever
        // `player.serverLevel().dimension()` is *now*, which may differ from the request's start.
        var complete = MapArtTileAccumulator.update(player, POS, NETHER, 62, 2, 3, tile(6));
        var c = assertInstanceOf(MapArtTileAccumulator.UpdateResult.Complete.class, complete);
        assertEquals(OVERWORLD, c.dimension(),
                "the dimension recorded when the transfer started must survive to completion, not the latest one seen");
    }

    @Test
    void discardRemovesThePendingTransfer() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, OVERWORLD, 30, 2, 0, tile(1));

        MapArtTileAccumulator.discard(player);

        // If discard had not run, this would be a duplicate of the tile admitted above.
        var result = MapArtTileAccumulator.update(player, POS, OVERWORLD, 30, 2, 0, tile(2));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, result);
    }

    @Test
    void sweepEvictsAnEntryThatWentQuietPastTheTimeout() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, OVERWORLD, 40, 2, 0, tile(1));

        MapArtTileAccumulator.sweepExpired(System.currentTimeMillis() + 100_000L);

        var result = MapArtTileAccumulator.update(player, POS, OVERWORLD, 40, 2, 0, tile(2));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Pending.class, result,
                "the sweep should have discarded the stale transfer rather than leave it in place");
    }

    @Test
    void sweepLeavesARecentTransferAlone() {
        UUID player = UUID.randomUUID();
        MapArtTileAccumulator.update(player, POS, OVERWORLD, 41, 2, 0, tile(1));

        MapArtTileAccumulator.sweepExpired(System.currentTimeMillis());

        var result = MapArtTileAccumulator.update(player, POS, OVERWORLD, 41, 2, 0, tile(2));
        assertInstanceOf(MapArtTileAccumulator.UpdateResult.Rejected.class, result,
                "a transfer that has not gone quiet must not be swept");
    }
}
