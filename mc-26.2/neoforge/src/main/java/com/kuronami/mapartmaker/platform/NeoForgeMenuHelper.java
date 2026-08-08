package com.kuronami.mapartmaker.platform;

import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.menu.MapArtMakerMenu;
import com.kuronami.mapartmaker.platform.services.IMenuHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

/**
 * NeoForge 実装: extended menu（BlockPos を client ctor へ運ぶ）。
 * MenuType は {@code IMenuTypeExtension.create}、open 時は buf に BlockPos を載せる。
 */
public class NeoForgeMenuHelper implements IMenuHelper {

    @Override
    public MenuType<MapArtMakerMenu> createMapArtMakerMenuType() {
        return IMenuTypeExtension.create((id, inv, buf) -> new MapArtMakerMenu(id, inv, buf.readBlockPos()));
    }

    @Override
    public void openMapArtMakerMenu(ServerPlayer player, BlockPos pos) {
        final BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof MapArtMakerBlockEntity maker)) {
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new MapArtMakerMenu(id, inv, maker),
                Component.translatable("container.map_art_maker")),
                buf -> buf.writeBlockPos(pos));
    }
}
