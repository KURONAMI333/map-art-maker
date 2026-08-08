package com.kuronami.mapartmaker.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Fabric での payload 型登録と server 側受信配線。client 側は client entry が張る。 */
public final class FabricPayloads {

    private FabricPayloads() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(CreateMapArtPayload.TYPE, CreateMapArtPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MapArtTilePayload.TYPE, MapArtTilePayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MapArtFeedbackPayload.TYPE, MapArtFeedbackPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CreateMapArtPayload.TYPE,
                (payload, context) -> ModNetwork.handleCreateMapArt(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(MapArtTilePayload.TYPE,
                (payload, context) -> MapArtTileAccumulator.handleTile(payload, context.player()));

        // Two more of the tile accumulator's three discard triggers; the third (GUI close) lives in
        // MapArtMakerMenu.removed, common to both loaders.
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                MapArtTileAccumulator.discard(handler.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> MapArtTileAccumulator.sweepExpired());
    }
}
