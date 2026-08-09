package com.kuronami.mapartmaker.gametest;

import net.neoforged.fml.common.Mod;

/**
 * GameTest を持つためだけの開発専用 mod。出荷 jar には入らない。
 *
 * <p>テストの登録は NeoForge の {@code GameTestHooks.registerGametests()} が
 * {@code ModFileScanData} から {@code @GameTestHolder} を拾って行うので、ここに登録の配線は無い。
 * この mod が存在する理由は「テストのクラスと structure を出荷物の外に置くこと」だけ
 * （{@code javafml} は modid に対応する {@code @Mod} クラスを1つ要求する）。
 *
 * <p>テスト ID の名前空間は {@code @GameTestHolder(Constants.MOD_ID)} のまま
 * ＝ {@code map_art_maker}。{@code neoforge.enabledGameTestNamespaces} はその名前空間で絞り込む
 * （この mod の id ではない）。
 */
@Mod(MapArtGameTestMod.MOD_ID)
public class MapArtGameTestMod {

    public static final String MOD_ID = "map_art_maker_gametest";

    public MapArtGameTestMod() {
    }
}
