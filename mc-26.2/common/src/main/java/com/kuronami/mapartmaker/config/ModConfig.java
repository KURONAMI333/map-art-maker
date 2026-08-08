package com.kuronami.mapartmaker.config;

import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;

/**
 * The two settings a server operator may want to change, with the defaults doing the right thing.
 *
 * <p>Loaders push their own config values in through {@link #apply}; anything that does not (or has
 * not loaded yet) leaves the defaults in place, so the mod is fully playable without a config file.
 */
public final class ModConfig {

    /**
     * Largest side of a request. The block has {@value MapArtMakerBlockEntity#OUTPUT_SLOTS} output
     * slots, so three is both the default and the ceiling — this exists to let an operator turn the
     * limit <em>down</em>, not up.
     */
    public static final int DEFAULT_MAX_TILES_PER_SIDE = 3;

    public static final int MAX_TILES_PER_SIDE_CEILING = 3;

    /**
     * Whether the server may fetch from addresses on its own network. Off by default: the URL comes
     * from a player, and the server is the one making the request, so leaving this open lets any
     * player probe the host's LAN and cloud metadata endpoints.
     */
    public static final boolean DEFAULT_ALLOW_PRIVATE_HOSTS = false;

    private static volatile int maxTilesPerSide = DEFAULT_MAX_TILES_PER_SIDE;
    private static volatile boolean allowPrivateHosts = DEFAULT_ALLOW_PRIVATE_HOSTS;

    private ModConfig() {
    }

    public static int maxTilesPerSide() {
        return maxTilesPerSide;
    }

    public static boolean allowPrivateHosts() {
        return allowPrivateHosts;
    }

    /** Called by each loader once its own config has loaded, and again on reload. */
    public static void apply(int newMaxTilesPerSide, boolean newAllowPrivateHosts) {
        maxTilesPerSide = clampTiles(newMaxTilesPerSide);
        allowPrivateHosts = newAllowPrivateHosts;
    }

    /**
     * Keeps the limit within what the block can actually hold. Going above the output slot count
     * would not produce bigger art, it would just make every request fail the free-slot check.
     */
    public static int clampTiles(int value) {
        if (value < 1) {
            return 1;
        }
        return Math.min(value, MAX_TILES_PER_SIDE_CEILING);
    }
}
