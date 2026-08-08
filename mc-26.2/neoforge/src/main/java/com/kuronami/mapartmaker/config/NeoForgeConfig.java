package com.kuronami.mapartmaker.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The NeoForge side of {@link ModConfig}.
 *
 * <p>Both entries are operator escape hatches rather than gameplay tuning, and the defaults match
 * the hard-coded behaviour, so a server that never opens this file behaves exactly as shipped.
 */
public final class NeoForgeConfig {

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue MAX_TILES_PER_SIDE;
    private static final ModConfigSpec.BooleanValue ALLOW_PRIVATE_HOSTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        MAX_TILES_PER_SIDE = builder
                .comment(
                        "Largest number of map tiles per side a single request may ask for.",
                        "The block has nine output slots, so 3 is the ceiling as well as the default.",
                        "Lower this to keep players from filling a world with large map art.")
                .defineInRange("maxTilesPerSide",
                        ModConfig.DEFAULT_MAX_TILES_PER_SIDE, 1, ModConfig.MAX_TILES_PER_SIDE_CEILING);

        ALLOW_PRIVATE_HOSTS = builder
                .comment(
                        "Allow image URLs that resolve to the server's own network.",
                        "Leave this off unless you are serving images from inside the same network:",
                        "the URL comes from a player and the server is what makes the request, so",
                        "turning it on lets players reach hosts only the server can see.")
                .define("allowPrivateHosts", ModConfig.DEFAULT_ALLOW_PRIVATE_HOSTS);

        SPEC = builder.build();
    }

    private NeoForgeConfig() {
    }

    /** Pushes the loaded values into the common holder. Safe to call again on reload. */
    public static void sync() {
        ModConfig.apply(MAX_TILES_PER_SIDE.get(), ALLOW_PRIVATE_HOSTS.get());
    }
}
