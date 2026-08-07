package com.kuronami.mapartmaker.client;

import com.kuronami.mapartmaker.network.MapArtFeedbackPayload;
import com.kuronami.mapartmaker.register.ModMenus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;

public class MapArtMakerFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.MAP_ART_MAKER.get(), MapArtMakerScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(MapArtFeedbackPayload.TYPE,
                (payload, context) -> MapArtFeedbackHolder.accept(payload));
    }
}
