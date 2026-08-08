package com.kuronami.mapartmaker;

import com.kuronami.mapartmaker.network.FabricPayloads;
import com.kuronami.mapartmaker.register.ModCreativeTab;
import com.kuronami.mapartmaker.register.ModRegistries;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

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
        // 26.2 の CreativeModeTab$Output は protected なので common では中身を書けない。
        // タブの identity だけ common が持ち、中身はこのイベントで流す。
        CreativeModeTabEvents.modifyOutputEvent(ModCreativeTab.TAB_KEY)
                .register(output -> ModCreativeTab.contents().forEach(output::accept));
        Constants.LOG.info("{} loaded", Constants.MOD_NAME);
    }
}
