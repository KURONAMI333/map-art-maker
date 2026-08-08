package com.kuronami.mapartmaker.platform;

import com.kuronami.mapartmaker.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        // 26.2 の FMLLoader.isProduction() は non-static。static 版は FMLEnvironment にある。
        return !FMLEnvironment.isProduction();
    }
}
