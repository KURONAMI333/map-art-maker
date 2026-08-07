package com.kuronami.mapartmaker.block;

import com.kuronami.mapartmaker.platform.Services;
import com.kuronami.mapartmaker.register.ModBlockEntities;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MapArtMakerBlock extends BaseEntityBlock {

    public static final MapCodec<MapArtMakerBlock> CODEC = simpleCodec(MapArtMakerBlock::new);

    public MapArtMakerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MapArtMakerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Services.MENU.openMapArtMakerMenu(serverPlayer, pos);
        }
        return InteractionResult.CONSUME;
    }

    /** Held maps must not vanish when the block is broken. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MapArtMakerBlockEntity maker) {
                Containers.dropContents(level, pos, maker);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Kept so the block entity type is initialised before the first placement. */
    public static void touchRegistration() {
        ModBlockEntities.MAP_ART_MAKER.get();
    }
}
