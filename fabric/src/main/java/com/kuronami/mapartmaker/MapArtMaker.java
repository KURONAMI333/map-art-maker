package com.kuronami.mapartmaker;

import com.kuronami.mapartmaker.network.FabricPayloads;
import com.kuronami.mapartmaker.register.ModRegistries;

import net.fabricmc.api.ModInitializer;

public class MapArtMaker implements ModInitializer {

    @Override
    public void onInitialize() {
        ModRegistries.init();
        FabricPayloads.register();
        Constants.LOG.info("{} loaded", Constants.MOD_NAME);
    }
}
