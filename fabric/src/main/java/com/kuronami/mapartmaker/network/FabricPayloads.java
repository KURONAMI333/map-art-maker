package com.kuronami.mapartmaker.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Fabric での payload 型登録と server 側受信配線。client 側は client entry が張る。 */
public final class FabricPayloads {

    private FabricPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(CreateMapArtPayload.TYPE, CreateMapArtPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MapArtFeedbackPayload.TYPE, MapArtFeedbackPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CreateMapArtPayload.TYPE,
                (payload, context) -> ModNetwork.handleCreateMapArt(payload, context.player()));
    }
}
