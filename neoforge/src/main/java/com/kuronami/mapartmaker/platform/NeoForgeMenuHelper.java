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

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

/**
 * NeoForge 実装: extended menu (BlockPos を client ctor へ運ぶ)。
 * MenuType は {@code IMenuTypeExtension.create} で生成し、open 時は buf に BlockPos を載せる。
 */
public class NeoForgeMenuHelper implements IMenuHelper {

    @Override
    public MenuType<MusicDiscMakerMenu> createMusicDiscMakerMenuType() {
        return IMenuTypeExtension.create(
                (id, inv, buf) -> new MusicDiscMakerMenu(id, inv, buf.readBlockPos()));
    }

    @Override
    public void openMusicDiscMakerMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> {
            final BlockEntity be = player.level().getBlockEntity(pos);
            return be instanceof MusicDiscMakerBlockEntity maker
                    ? new MusicDiscMakerMenu(id, inv, maker)
                    : null;
        }, Component.translatable("gui.music_disc_maker.title")), buf -> buf.writeBlockPos(pos));
    }

    @Override
    public MenuType<GoldenJukeboxMenu> createGoldenJukeboxMenuType() {
        return IMenuTypeExtension.create(
                (id, inv, buf) -> new GoldenJukeboxMenu(id, inv, buf.readBlockPos()));
    }

    @Override
    public void openGoldenJukeboxMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> {
            final BlockEntity be = player.level().getBlockEntity(pos);
            return be instanceof GoldenJukeboxBlockEntity jukebox
                    ? new GoldenJukeboxMenu(id, inv, jukebox)
                    : null;
        }, Component.translatable("gui.music_disc_maker.golden_jukebox.title")), buf -> buf.writeBlockPos(pos));
    }

    @Override
    public MenuType<PlaylistBinderMenu> createPlaylistBinderMenuType() {
        return IMenuTypeExtension.create(
                (id, inv, buf) -> new PlaylistBinderMenu(id, inv, buf.readBlockPos()));
    }

    @Override
    public void openPlaylistBinderMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> {
            final BlockEntity be = player.level().getBlockEntity(pos);
            return be instanceof PlaylistBinderBlockEntity binder
                    ? new PlaylistBinderMenu(id, inv, binder)
                    : null;
        }, Component.translatable("gui.music_disc_maker.binder.title")), buf -> buf.writeBlockPos(pos));
    }

    @Override
    public MenuType<SpeakerMenu> createSpeakerMenuType() {
        return IMenuTypeExtension.create(
                (id, inv, buf) -> new SpeakerMenu(id, inv, buf.readBlockPos()));
    }

    @Override
    public void openSpeakerMenu(ServerPlayer player, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> {
            final BlockEntity be = player.level().getBlockEntity(pos);
            return be instanceof SpeakerBlockEntity speaker ? new SpeakerMenu(id, speaker) : null;
        }, Component.translatable("gui.music_disc_maker.speaker.title")), buf -> buf.writeBlockPos(pos));
    }

    @Override
    public MenuType<BoomboxMenu> createBoomboxMenuType() {
        return IMenuTypeExtension.create(
                (id, inv, buf) -> new BoomboxMenu(id, inv, BoomboxMenuData.STREAM_CODEC.decode(buf)));
    }

    @Override
    public void openBoomboxMenu(ServerPlayer player, BoomboxMenuData data) {
        // ブロックに紐づかないので BlockEntity は引かない。menu 側が個体 UUID で解決する。
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new BoomboxMenu(id, inv, data),
                Component.translatable("gui.music_disc_maker.boombox.title")),
                buf -> BoomboxMenuData.STREAM_CODEC.encode(buf, data));
    }
}
