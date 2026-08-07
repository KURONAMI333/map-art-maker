package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.Constants;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** server → client: 生成の結果（GUI に出す1行）。 */
public record MapArtFeedbackPayload(boolean success, Component message) implements CustomPacketPayload {

    public static final Type<MapArtFeedbackPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "map_art_feedback"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MapArtFeedbackPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, MapArtFeedbackPayload::success,
                    ComponentSerialization.STREAM_CODEC, MapArtFeedbackPayload::message,
                    MapArtFeedbackPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
