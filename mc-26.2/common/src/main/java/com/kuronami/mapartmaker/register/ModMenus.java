package com.kuronami.mapartmaker.register;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.menu.MapArtMakerMenu;
import com.kuronami.mapartmaker.platform.Services;
import com.kuronami.mapartmaker.platform.registry.RegistrationProvider;
import com.kuronami.mapartmaker.platform.registry.RegistryHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {

    public static final RegistrationProvider<MenuType<?>> MENUS =
            RegistrationProvider.get(Registries.MENU, Constants.MOD_ID);

    // extended MenuType の生成は loader 固有。登録は generic provider、値だけ Service が供給する。
    public static final RegistryHolder<MenuType<MapArtMakerMenu>> MAP_ART_MAKER =
            MENUS.register("map_art_maker", () -> Services.MENU.createMapArtMakerMenuType());

    private ModMenus() {
    }

    public static void init() {
    }
}
