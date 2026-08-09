package com.kuronami.mapartmaker.gametest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * GameTest を登録するためだけの開発専用 mod。出荷 jar には入らない。
 *
 * <p>26.x の {@code GameTestInstance} は実レジストリ {@code minecraft:test_instance} に載り、
 * そのレジストリはクライアント join の configuration フェーズで同期される。製品の mod が
 * テストを登録すると、利用者の環境でワールド生成が configuration フェーズのまま固まる
 * （2026-08-09 実測）。テストの登録は本体 mod から切り離し、この source set
 * （{@code neoforge/src/gametest}）ごと出荷物の外に置いてある。
 *
 * <p>テスト ID の名前空間は本体と同じ {@code map_art_maker} のままにする
 * （{@code neoforge.enabledGameTestNamespaces} がその名前空間で絞り込むため。
 * この mod の id ではない）。
 */
@Mod(MapArtGameTestMod.MOD_ID)
public class MapArtGameTestMod {

    public static final String MOD_ID = "map_art_maker_gametest";

    public MapArtGameTestMod(IEventBus eventBus) {
        eventBus.addListener(MapArtGameTestRegistration::register);
    }
}
