package com.kuronami.mapartmaker.platform;

import com.kuronami.mapartmaker.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;

import com.kuronami.mapartmaker.platform.services.ModBlockEntitySupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(ModBlockEntitySupplier<T> supplier, Block block) {
        return BlockEntityType.Builder.<T>of(supplier::create, block).build(null);
    }


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

        return !FMLLoader.isProduction();
    }
}