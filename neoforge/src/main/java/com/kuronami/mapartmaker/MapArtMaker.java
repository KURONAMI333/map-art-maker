package com.kuronami.mapartmaker;

import com.kuronami.mapartmaker.client.MapArtMakerScreen;
import com.kuronami.mapartmaker.config.NeoForgeConfig;
import com.kuronami.mapartmaker.network.NeoForgePayloads;
import com.kuronami.mapartmaker.platform.registry.NeoForgeRegistrationProvider;
import com.kuronami.mapartmaker.register.ModMenus;
import com.kuronami.mapartmaker.register.ModRegistries;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Constants.MOD_ID)
public class MapArtMaker {

    public MapArtMaker(IEventBus eventBus, ModContainer container) {
        // init() runs the holders' static initialisers, which is what creates the deferred
        // registers; they do nothing until they are bound to the bus, so the order matters.
        ModRegistries.init();
        NeoForgeRegistrationProvider.registerAll(eventBus);
        eventBus.addListener(NeoForgePayloads::register);
        container.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC);
        // COMMON rather than SERVER so the client knows the tile limit its size button cycles
        // through. The server revalidates every request regardless.
        eventBus.addListener((ModConfigEvent.Loading event) -> NeoForgeConfig.sync());
        eventBus.addListener((ModConfigEvent.Reloading event) -> NeoForgeConfig.sync());
        // Game-bus lifecycle hooks for the file-drop tile accumulator: disconnect and the periodic
        // timeout sweep. These fire on the game bus (NeoForge.EVENT_BUS), not the mod bus above.
        NeoForge.EVENT_BUS.addListener(NeoForgePayloads::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(NeoForgePayloads::onServerTick);
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
