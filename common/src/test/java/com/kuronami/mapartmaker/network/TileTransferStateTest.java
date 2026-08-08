package com.kuronami.mapartmaker.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileTransferStateTest {

    private static byte[] tileBytes(int fill) {
        byte[] out = new byte[TileTransferState.TILE_BYTES];
        java.util.Arrays.fill(out, (byte) fill);
        return out;
    }

    @Test
    void splitThenReassembleIsByteForByteIdentical() {
        TileTransferState state = new TileTransferState(1, 2, 2, false);
        byte[][] expected = {tileBytes(1), tileBytes(2), tileBytes(3), tileBytes(4)};

        for (int i = 0; i < expected.length; i++) {
            boolean complete = state.admit(i, expected[i]);
            assertEquals(i == expected.length - 1, complete, "only the last tile should complete the transfer");
        }

        byte[][] actual = state.tilesInOrder();
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], "tile " + i + " changed on the way through");
        }
    }

    @Test
    void tilesCanArriveOutOfOrder() {
        TileTransferState state = new TileTransferState(1, 2, 2, false);
        byte[] tile3 = tileBytes(30);
        byte[] tile0 = tileBytes(0);

        assertFalse(state.admit(3, tile3));
        assertFalse(state.isComplete());
        assertFalse(state.admit(0, tile0));

        byte[][] actual = state.tilesInOrder();
        assertArrayEquals(tile0, actual[0]);
        assertArrayEquals(tile3, actual[3]);
    }

    @Test
    void rejectsNegativeTileIndex() {
        TileTransferState state = new TileTransferState(1, 2, 2, false);
        assertThrows(IllegalArgumentException.class, () -> state.admit(-1, tileBytes(1)));
    }

    @Test
    void rejectsTileIndexAtOrPastTheCount() {
        TileTransferState state = new TileTransferState(1, 2, 2, false);
        assertThrows(IllegalArgumentException.class, () -> state.admit(4, tileBytes(1)));
    }

    @Test
    void rejectsTheWrongColourByteCount() {
        TileTransferState state = new TileTransferState(1, 1, 1, false);
        assertThrows(IllegalArgumentException.class, () -> state.admit(0, new byte[100]));
    }

    @Test
    void rejectsADuplicateTileIndex() {
        TileTransferState state = new TileTransferState(1, 1, 1, false);
        state.admit(0, tileBytes(9));
        assertThrows(IllegalStateException.class, () -> state.admit(0, tileBytes(9)));
    }

    @Test
    void isCompleteOnlyOnceEveryTileArrived() {
        TileTransferState state = new TileTransferState(1, 1, 3, false);
        assertFalse(state.isComplete());
        state.admit(0, tileBytes(1));
        assertFalse(state.isComplete());
        state.admit(1, tileBytes(2));
        assertFalse(state.isComplete());
        state.admit(2, tileBytes(3));
        assertTrue(state.isComplete());
    }

    @Test
    void rejectsNonPositiveTileCounts() {
        assertThrows(IllegalArgumentException.class, () -> new TileTransferState(1, 0, 1, false));
        assertThrows(IllegalArgumentException.class, () -> new TileTransferState(1, 1, -1, false));
    }
}
