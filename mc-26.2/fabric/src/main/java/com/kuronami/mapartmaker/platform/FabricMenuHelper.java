package com.kuronami.mapartmaker.platform;

import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.menu.MapArtMakerMenu;
import com.kuronami.mapartmaker.platform.services.IMenuHelper;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
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
 *
 * <p>26.x で {@code fabric-screen-handler-api-v1} は廃止され {@code fabric-menu-api-v1} に置き換わった。
 * 型が {@code ExtendedScreenHandlerType}/{@code ExtendedScreenHandlerFactory} から
 * {@code ExtendedMenuType}/{@code ExtendedMenuProvider} に変わっただけで、D=BlockPos の運び方は同型。
 */
public class FabricMenuHelper implements IMenuHelper {

    @Override
    public MenuType<MapArtMakerMenu> createMapArtMakerMenuType() {
        return new ExtendedMenuType<>(
                (id, inv, pos) -> new MapArtMakerMenu(id, inv, pos), BlockPos.STREAM_CODEC);
    }

    @Override
    public void openMapArtMakerMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new ExtendedMenuProvider<BlockPos>() {

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
