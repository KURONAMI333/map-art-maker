package com.kuronami.mapartmaker.register;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.block.MapArtMakerBlock;
import com.kuronami.mapartmaker.platform.registry.RegistrationProvider;
import com.kuronami.mapartmaker.platform.registry.RegistryHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {

    public static final RegistrationProvider<Block> BLOCKS =
            RegistrationProvider.get(Registries.BLOCK, Constants.MOD_ID);

    // 製図台と同じ「作業台」枠。硬さ・素材・適性ツール・音を合わせる。
    public static final RegistryHolder<MapArtMakerBlock> MAP_ART_MAKER =
            BLOCKS.register("map_art_maker",
                    () -> new MapArtMakerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CARTOGRAPHY_TABLE)));

    private ModBlocks() {
    }

    public static void init() {
    }
}
