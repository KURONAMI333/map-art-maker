package com.kuronami.mapartmaker.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * payload 送信の loader 抽象。型の登録と受信配線は各ローダーの entry で行い、
 * 受信時は common の {@link com.kuronami.mapartmaker.network.ModNetwork} へ委譲する。
 */
public interface INetworkHelper {

    /** client → server。client 専用経路。 */
    void sendToServer(CustomPacketPayload payload);

    /** server → 特定 player。 */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
