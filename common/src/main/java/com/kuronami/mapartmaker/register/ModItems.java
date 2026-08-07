package com.kuronami.mapartmaker.register;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.platform.registry.RegistrationProvider;
import com.kuronami.mapartmaker.platform.registry.RegistryHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public final class ModItems {

    public static final RegistrationProvider<Item> ITEMS =
            RegistrationProvider.get(Registries.ITEM, Constants.MOD_ID);

    public static final RegistryHolder<BlockItem> MAP_ART_MAKER =
            ITEMS.register("map_art_maker",
                    () -> new BlockItem(ModBlocks.MAP_ART_MAKER.get(), new Item.Properties()));

    private ModItems() {
    }

    public static void init() {
    }
}
