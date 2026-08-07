package com.kuronami.mapartmaker.register;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.platform.Services;
import com.kuronami.mapartmaker.platform.registry.RegistrationProvider;
import com.kuronami.mapartmaker.platform.registry.RegistryHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    // BlockEntityType の生成は loader 固有（vanilla builder の supplier が package-private）。
    public static final RegistryHolder<BlockEntityType<MapArtMakerBlockEntity>> MAP_ART_MAKER =
            BLOCK_ENTITIES.register("map_art_maker",
                    () -> Services.PLATFORM.createBlockEntityType(
                            MapArtMakerBlockEntity::new, ModBlocks.MAP_ART_MAKER.get()));

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
