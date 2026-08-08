package com.kuronami.mapartmaker.register;

import java.util.Set;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.platform.registry.RegistrationProvider;
import com.kuronami.mapartmaker.platform.registry.RegistryHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    // 26.2 の BlockEntityType は (BlockEntitySupplier, Set<Block>) ctor だけを持ち、ctor も
    // BlockEntitySupplier も public。common（vanilla classpath）から直接生成できるので、
    // 1.21.1 で要った createBlockEntityType の loader SPI は不要。
    public static final RegistryHolder<BlockEntityType<MapArtMakerBlockEntity>> MAP_ART_MAKER =
            BLOCK_ENTITIES.register("map_art_maker",
                    () -> new BlockEntityType<>(MapArtMakerBlockEntity::new,
                            Set.of(ModBlocks.MAP_ART_MAKER.get())));

    private ModBlockEntities() {
    }

    public static void init() {
    }
}
