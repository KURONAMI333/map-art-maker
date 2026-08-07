package com.kuronami.mapartmaker.register;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.platform.registry.RegistrationProvider;
import com.kuronami.mapartmaker.platform.registry.RegistryHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTab {

    public static final RegistrationProvider<CreativeModeTab> TABS =
            RegistrationProvider.get(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    public static final RegistryHolder<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup." + Constants.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.MAP_ART_MAKER.get()))
                    .displayItems((params, output) -> output.accept(ModItems.MAP_ART_MAKER.get()))
                    .build());

    private ModCreativeTab() {
    }

    public static void init() {
    }
}
