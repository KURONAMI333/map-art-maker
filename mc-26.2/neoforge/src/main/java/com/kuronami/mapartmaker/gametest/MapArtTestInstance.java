package com.kuronami.mapartmaker.gametest;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 26.x の GameTest は「アノテーションの付いた static メソッド」ではなく、レジストリに載る
 * {@link GameTestInstance} が単位になった（{@code net.minecraft.gametest.framework.GameTest} と
 * NeoForge の {@code @GameTestHolder}/{@code @PrefixGameTestTemplate} は 26.2 に存在しない）。
 *
 * <p>ここではテスト本体を {@code Consumer<GameTestHelper>} として受け取る薄い実装だけを置き、
 * 1.21.1 セルのテスト本体（{@link MapArtGameTests} / {@code MapArtNetworkGameTests} の static メソッド）を
 * そのまま流用できるようにする。
 *
 * <p>{@link #codec()} は datapack 経由の直列化用で、コードから直接登録するこの経路では読まれない。
 * 直列化が要る用途（{@code /test} コマンドの export 等）には使えない。
 */
public class MapArtTestInstance extends GameTestInstance {

    private final String name;
    private final Consumer<GameTestHelper> body;

    public MapArtTestInstance(String name,
                              TestData<Holder<TestEnvironmentDefinition<?>>> info,
                              Consumer<GameTestHelper> body) {
        super(info);
        this.name = name;
        this.body = body;
    }

    @Override
    public void run(GameTestHelper helper) {
        this.body.accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("map art maker test: " + this.name);
    }
}
