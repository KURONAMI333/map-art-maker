package com.kuronami.mapartmaker.network;

import com.kuronami.mapartmaker.client.MapArtFeedbackHolder;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge での payload 型登録と受信配線。受信は main thread なので、そのまま common の
 * {@link ModNetwork} へ委譲する（重い処理は ModNetwork 側が別スレッドへ逃がす）。
 */
public final class NeoForgePayloads {

    private NeoForgePayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(CreateMapArtPayload.TYPE, CreateMapArtPayload.STREAM_CODEC,
                NeoForgePayloads::handleCreate);
        registrar.playToServer(MapArtTilePayload.TYPE, MapArtTilePayload.STREAM_CODEC,
                NeoForgePayloads::handleTile);
        registrar.playToClient(MapArtFeedbackPayload.TYPE, MapArtFeedbackPayload.STREAM_CODEC,
                NeoForgePayloads::handleFeedback);
    }

    private static void handleCreate(CreateMapArtPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ModNetwork.handleCreateMapArt(payload, player);
        }
    }

    private static void handleTile(MapArtTilePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            MapArtTileAccumulator.handleTile(payload, player);
        }
    }

    private static void handleFeedback(MapArtFeedbackPayload payload, IPayloadContext context) {
        MapArtFeedbackHolder.accept(payload);
    }

    /** One of the tile accumulator's three discard triggers; see {@link MapArtTileAccumulator}. */
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        MapArtTileAccumulator.discard(event.getEntity().getUUID());
    }

    /** The timeout trigger: cheap, and a no-op unless a transfer is actually in flight. */
    public static void onServerTick(ServerTickEvent.Post event) {
        MapArtTileAccumulator.sweepExpired();
    }
}
