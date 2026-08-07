package com.kuronami.mapartmaker;

import com.kuronami.mapartmaker.network.FabricPayloads;
import com.kuronami.mapartmaker.register.ModRegistries;

import net.fabricmc.api.ModInitializer;

/**
 * The Fabric entrypoint runs on {@link com.kuronami.mapartmaker.config.ModConfig}'s defaults.
 *
 * <p>Shipping targets NeoForge, where the Fabric side of the map-art niche is already served, so no
 * Fabric config backend is wired up. The defaults are the shipped behaviour rather than a fallback,
 * so this build is complete on its own; if Fabric ever becomes a release target, the work is one
 * config backend calling {@code ModConfig.apply}.
 */
public class MapArtMaker implements ModInitializer {

    @Override
    public void onInitialize() {
        ModRegistries.init();
        FabricPayloads.register();
        Constants.LOG.info("{} loaded", Constants.MOD_NAME);
    }
}
