package com.kuronami.mapartmaker.register;

import java.util.List;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.platform.registry.RegistrationProvider;
import com.kuronami.mapartmaker.platform.registry.RegistryHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * タブの identity（title / icon）だけを common で作る。
 * 26.2 の {@code CreativeModeTab$Output} は protected static なので、{@code displayItems} generator を
 * common（vanilla classpath）から書けない。中身は各ローダーのタブ内容イベントで {@link #contents()} を流す
 * （NeoForge = {@code BuildCreativeModeTabContentsEvent} / Fabric = {@code CreativeModeTabEvents}）。
 */
public final class ModCreativeTab {

    public static final RegistrationProvider<CreativeModeTab> TABS =
            RegistrationProvider.get(Registries.CREATIVE_MODE_TAB, Constants.MOD_ID);

    /** ローダー側のタブ内容イベントが対象タブを指すためのキー。 */
    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "main"));

    public static final RegistryHolder<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup." + Constants.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.MAP_ART_MAKER.get()))
                    .build());

    /** タブに並ぶもの。1.21.1 の displayItems と同じ内容を1箇所で持つ。 */
    public static List<ItemStack> contents() {
        return List.of(new ItemStack(ModItems.MAP_ART_MAKER.get()));
    }

    private ModCreativeTab() {
    }

    public static void init() {
    }
}
