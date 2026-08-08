package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.mapart.MapArtService;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * client → server: ドロップ経路の1タイル分。クライアントで量子化済みの色バイトをそのまま運ぶ。
 *
 * <p>3x3 の全タイルは 147,456 バイトで {@code ServerboundCustomPayloadPacket} の上限
 * 32,767 バイトを超えるので、1パケット1タイル（{@value #TILE_BYTES} バイト）に割って送る。
 * {@code tilesX}/{@code tilesY} は GUI が常に正方形（サイズボタンは1辺のタイル数）しか作らないので
 * {@code size} 1本にまとめている（{@link StreamCodec#composite} は6フィールドが上限のため）。
 */
public record MapArtTilePayload(BlockPos pos, int transferId, int size, int tileIndex,
                                 byte[] colours) implements CustomPacketPayload {

    /** {@code MapArtService.TILE * MapArtService.TILE}. 地図1枚分の色バイト数。 */
    public static final int TILE_BYTES = MapArtService.TILE * MapArtService.TILE;

    public static final Type<MapArtTilePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "map_art_tile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapArtTilePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MapArtTilePayload::pos,
                    ByteBufCodecs.VAR_INT, MapArtTilePayload::transferId,
                    ByteBufCodecs.VAR_INT, MapArtTilePayload::size,
                    ByteBufCodecs.VAR_INT, MapArtTilePayload::tileIndex,
                    ByteBufCodecs.byteArray(TILE_BYTES), MapArtTilePayload::colours,
                    MapArtTilePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
