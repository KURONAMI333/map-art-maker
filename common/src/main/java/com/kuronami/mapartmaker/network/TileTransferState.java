package com.kuronami.mapartmaker.network;

/**
 * Pure tile-reassembly state for one in-flight upload. No Minecraft types, so the admission rules
 * are unit testable without a running game; {@link MapArtTileAccumulator} is the Minecraft-facing
 * layer that owns one of these per player.
 */
final class TileTransferState {

    /** 128x128, {@code MapArtService.TILE * MapArtService.TILE} duplicated so this stays MC-free. */
    static final int TILE_BYTES = 128 * 128;

    private final int transferId;
    private final int tilesX;
    private final int tilesY;
    private final boolean dither;
    private final byte[][] tiles;
    private final boolean[] received;
    private int receivedCount;

    TileTransferState(int transferId, int tilesX, int tilesY, boolean dither) {
        if (tilesX <= 0 || tilesY <= 0) {
            throw new IllegalArgumentException("tile counts must be positive");
        }
        this.transferId = transferId;
        this.tilesX = tilesX;
        this.tilesY = tilesY;
        this.dither = dither;
        int count = tilesX * tilesY;
        this.tiles = new byte[count][];
        this.received = new boolean[count];
    }

    int transferId() {
        return transferId;
    }

    int tilesX() {
        return tilesX;
    }

    int tilesY() {
        return tilesY;
    }

    boolean dither() {
        return dither;
    }

    int tileCount() {
        return tiles.length;
    }

    boolean isComplete() {
        return receivedCount == tiles.length;
    }

    /**
     * @return true if this call completed the transfer
     * @throws IllegalArgumentException tileIndex out of range, or colours the wrong length
     * @throws IllegalStateException    tileIndex was already received
     */
    boolean admit(int tileIndex, byte[] colours) {
        if (tileIndex < 0 || tileIndex >= tiles.length) {
            throw new IllegalArgumentException(
                    "tile index " + tileIndex + " out of range for " + tiles.length + " tiles");
        }
        if (colours.length != TILE_BYTES) {
            throw new IllegalArgumentException(
                    "expected " + TILE_BYTES + " colour bytes, got " + colours.length);
        }
        if (received[tileIndex]) {
            throw new IllegalStateException("tile " + tileIndex + " was already received");
        }
        tiles[tileIndex] = colours;
        received[tileIndex] = true;
        receivedCount++;
        return isComplete();
    }

    /** Only meaningful once {@link #isComplete()}; unreceived slots are {@code null} otherwise. */
    byte[][] tilesInOrder() {
        return tiles.clone();
    }
}
