package com.kuronami.mapartmaker.platform;

import com.kuronami.mapartmaker.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

import com.kuronami.mapartmaker.platform.services.ModBlockEntitySupplier;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(ModBlockEntitySupplier<T> supplier, Block block) {
        return FabricBlockEntityTypeBuilder.<T>create(supplier::create, block).build();
    }


    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
