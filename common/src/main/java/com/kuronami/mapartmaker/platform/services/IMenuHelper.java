package com.kuronami.mapartmaker.platform.services;

import com.kuronami.mapartmaker.menu.MapArtMakerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;

/**
 * extended menu の loader 抽象（開く時に BlockPos を client ctor へ運ぶ）。
 * NeoForge は {@code IMenuTypeExtension}、Fabric は {@code ExtendedScreenHandlerType}。
 */
public interface IMenuHelper {

    MenuType<MapArtMakerMenu> createMapArtMakerMenuType();

    void openMapArtMakerMenu(ServerPlayer player, BlockPos pos);
}
