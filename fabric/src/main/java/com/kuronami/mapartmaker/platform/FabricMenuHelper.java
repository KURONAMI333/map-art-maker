package com.kuronami.mapartmaker.platform;

import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.menu.MapArtMakerMenu;
import com.kuronami.mapartmaker.platform.services.IMenuHelper;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fabric 実装: extended menu（BlockPos を client ctor へ運ぶ）。
 */
public class FabricMenuHelper implements IMenuHelper {

    @Override
    public MenuType<MapArtMakerMenu> createMapArtMakerMenuType() {
        return new ExtendedScreenHandlerType<>(
                (id, inv, pos) -> new MapArtMakerMenu(id, inv, pos), BlockPos.STREAM_CODEC);
    }

    @Override
    public void openMapArtMakerMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {

            @Override
            public BlockPos getScreenOpeningData(ServerPlayer p) {
                return pos;
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("container.map_art_maker");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                final BlockEntity be = p.level().getBlockEntity(pos);
                return be instanceof MapArtMakerBlockEntity maker ? new MapArtMakerMenu(id, inv, maker) : null;
            }
        });
    }
}
