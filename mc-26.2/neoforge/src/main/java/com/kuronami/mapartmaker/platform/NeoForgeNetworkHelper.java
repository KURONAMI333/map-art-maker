package com.kuronami.mapartmaker.platform;

import com.kuronami.mapartmaker.platform.services.INetworkHelper;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientSender.send(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    /**
     * 26.2 の client→server 送信は client 専用の {@code ClientPacketDistributor} に移った。
     * この helper 自体は ServiceLoader 経由で両 dist に載るので、client 専用クラスへの参照は
     * 別クラスに閉じ込め、client 経路（画面・ドロップ）から呼ばれた時だけ解決されるようにする。
     *
     * <p>専用サーバーはこのメソッドを一度も実行しないため、JVM の遅延解決によりロードもされない。
     * ヘッドレスや GameTest はこの分離を検証できない（実行されない経路のため）。
     */
    private static final class ClientSender {

        private ClientSender() {
        }

        static void send(CustomPacketPayload payload) {
            net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
        }
    }
}
