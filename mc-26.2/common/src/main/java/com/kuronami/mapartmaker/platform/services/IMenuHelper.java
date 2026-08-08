package com.kuronami.mapartmaker.platform.services;

import com.kuronami.mapartmaker.menu.MapArtMakerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;

/**
 * extended menu の loader 抽象（開く時に BlockPos を client ctor へ運ぶ）。
 *
 * <p>付随データを付けて menu を開く仕組みは vanilla に無く、各ローダーが別の API で提供する。
 * この interface はその2手順（MenuType の生成と、サーバから開くときに BlockPos を載せること）だけを
 * 定め、実装は各ローダーの platform パッケージに置く。ローダー固有の型名はここに書かない
 * （版やローダーで型名が変わるので、このファイルをセル間で同一に保てなくなる）。
 */
public interface IMenuHelper {

    MenuType<MapArtMakerMenu> createMapArtMakerMenuType();

    void openMapArtMakerMenu(ServerPlayer player, BlockPos pos);
}
