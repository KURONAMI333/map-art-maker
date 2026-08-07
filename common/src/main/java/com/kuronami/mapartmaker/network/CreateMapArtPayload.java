package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.Constants;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** client → server: この URL を、この枚数で地図にしてくれ。 */
public record CreateMapArtPayload(BlockPos pos, String url, int tilesX, int tilesY, boolean dither)
        implements CustomPacketPayload {

    /** URL の長さ上限。悪意ある巨大文字列でパケットを膨らませない。 */
    public static final int MAX_URL_LENGTH = 1024;

    public static final Type<CreateMapArtPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "create_map_art"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateMapArtPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CreateMapArtPayload::pos,
                    ByteBufCodecs.stringUtf8(MAX_URL_LENGTH), CreateMapArtPayload::url,
                    ByteBufCodecs.VAR_INT, CreateMapArtPayload::tilesX,
                    ByteBufCodecs.VAR_INT, CreateMapArtPayload::tilesY,
                    ByteBufCodecs.BOOL, CreateMapArtPayload::dither,
                    CreateMapArtPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
