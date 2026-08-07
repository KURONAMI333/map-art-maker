package com.kuronami.mapartmaker;

import com.kuronami.mapartmaker.client.MapArtMakerScreen;
import com.kuronami.mapartmaker.network.NeoForgePayloads;
import com.kuronami.mapartmaker.register.ModMenus;
import com.kuronami.mapartmaker.register.ModRegistries;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(Constants.MOD_ID)
public class MapArtMaker {

    public MapArtMaker(IEventBus eventBus) {
        ModRegistries.init();
        eventBus.addListener(NeoForgePayloads::register);
        Constants.LOG.info("{} loaded", Constants.MOD_NAME);
    }

    /** client 専用の配線。dedicated server では never-load。 */
    @Mod(value = Constants.MOD_ID, dist = Dist.CLIENT)
    public static class Client {

        public Client(IEventBus eventBus) {
            eventBus.addListener(Client::registerScreens);
        }

        private static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenus.MAP_ART_MAKER.get(), MapArtMakerScreen::new);
        }
    }
}
