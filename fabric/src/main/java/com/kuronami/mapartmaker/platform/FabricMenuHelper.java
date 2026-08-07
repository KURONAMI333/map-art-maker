package com.kuronami.mapartmaker.platform;

import com.kuronami.mapartmaker.block.GoldenJukeboxBlockEntity;
import com.kuronami.mapartmaker.block.MusicDiscMakerBlockEntity;
import com.kuronami.mapartmaker.block.SpeakerBlockEntity;
import com.kuronami.mapartmaker.menu.BoomboxMenu;
import com.kuronami.mapartmaker.menu.BoomboxMenuData;
import com.kuronami.mapartmaker.block.PlaylistBinderBlockEntity;
import com.kuronami.mapartmaker.menu.GoldenJukeboxMenu;
import com.kuronami.mapartmaker.menu.PlaylistBinderMenu;
import com.kuronami.mapartmaker.menu.MusicDiscMakerMenu;
import com.kuronami.mapartmaker.menu.SpeakerMenu;
import com.kuronami.mapartmaker.platform.services.IMenuHelper;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fabric 実装: extended menu (BlockPos を client ctor へ運ぶ)。
 * MenuType は {@link ExtendedScreenHandlerType}、open 時は {@link ExtendedScreenHandlerFactory} で pos を載せる。
 */
public class FabricMenuHelper implements IMenuHelper {

    @Override
    public MenuType<MusicDiscMakerMenu> createMusicDiscMakerMenuType() {
        return new ExtendedScreenHandlerType<>(
                (id, inv, pos) -> new MusicDiscMakerMenu(id, inv, pos), BlockPos.STREAM_CODEC);
    }

    @Override
    public void openMusicDiscMakerMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {

            @Override
            public BlockPos getScreenOpeningData(ServerPlayer p) {
                return pos;
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.music_disc_maker.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                final BlockEntity be = p.level().getBlockEntity(pos);
                return be instanceof MusicDiscMakerBlockEntity maker
                        ? new MusicDiscMakerMenu(id, inv, maker)
                        : null;
            }
        });
    }

    @Override
    public MenuType<GoldenJukeboxMenu> createGoldenJukeboxMenuType() {
        return new ExtendedScreenHandlerType<>(
                (id, inv, pos) -> new GoldenJukeboxMenu(id, inv, pos), BlockPos.STREAM_CODEC);
    }

    @Override
    public void openGoldenJukeboxMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {

            @Override
            public BlockPos getScreenOpeningData(ServerPlayer p) {
                return pos;
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.music_disc_maker.golden_jukebox.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                final BlockEntity be = p.level().getBlockEntity(pos);
                return be instanceof GoldenJukeboxBlockEntity jukebox
                        ? new GoldenJukeboxMenu(id, inv, jukebox)
                        : null;
            }
        });
    }

    @Override
    public MenuType<PlaylistBinderMenu> createPlaylistBinderMenuType() {
        return new ExtendedScreenHandlerType<>(
                (id, inv, pos) -> new PlaylistBinderMenu(id, inv, pos), BlockPos.STREAM_CODEC);
    }

    @Override
    public void openPlaylistBinderMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {

            @Override
            public BlockPos getScreenOpeningData(ServerPlayer p) {
                return pos;
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.music_disc_maker.binder.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                final BlockEntity be = p.level().getBlockEntity(pos);
                return be instanceof PlaylistBinderBlockEntity binder
                        ? new PlaylistBinderMenu(id, inv, binder)
                        : null;
            }
        });
    }

    @Override
    public MenuType<SpeakerMenu> createSpeakerMenuType() {
        return new ExtendedScreenHandlerType<>(
                (id, inv, pos) -> new SpeakerMenu(id, inv, pos), BlockPos.STREAM_CODEC);
    }

    @Override
    public void openSpeakerMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {

            @Override
            public BlockPos getScreenOpeningData(ServerPlayer p) {
                return pos;
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.music_disc_maker.speaker.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                final BlockEntity be = p.level().getBlockEntity(pos);
                return be instanceof SpeakerBlockEntity speaker ? new SpeakerMenu(id, speaker) : null;
            }
        });
    }

    @Override
    public MenuType<BoomboxMenu> createBoomboxMenuType() {
        return new ExtendedScreenHandlerType<>(
                (id, inv, data) -> new BoomboxMenu(id, inv, data), BoomboxMenuData.STREAM_CODEC);
    }

    @Override
    public void openBoomboxMenu(ServerPlayer player, BoomboxMenuData data) {
        player.openMenu(new ExtendedScreenHandlerFactory<BoomboxMenuData>() {

            @Override
            public BoomboxMenuData getScreenOpeningData(ServerPlayer p) {
                return data;
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("gui.music_disc_maker.boombox.title");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                // ブロックに紐づかないので BlockEntity は引かない。menu 側が個体 UUID で解決する。
                return new BoomboxMenu(id, inv, data);
            }
        });
    }
}
