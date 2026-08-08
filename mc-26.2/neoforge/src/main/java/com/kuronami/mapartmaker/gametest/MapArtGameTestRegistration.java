package com.kuronami.mapartmaker.gametest;

import java.util.List;
import java.util.function.Consumer;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.network.MapArtNetworkGameTests;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * 26.x でのテスト登録。{@link RegisterGameTestsEvent#registerTest} にインスタンスを渡す方式で、
 * 1.21.1 セルのアノテーション（{@code @GameTestHolder} + {@code @GameTest(template=...)}）を置き換える。
 *
 * <p>全 13 件が 1.21.1 セルと同じ本体・同じ構造（{@code map_art_maker:empty3x3x3}）を使う。
 * 件数が変わったらこの一覧が正本なので、テストを足したらここにも足す。
 */
public final class MapArtGameTestRegistration {

    /** 1.21.1 の {@code @GameTest} 既定値と同じ。 */
    private static final int MAX_TICKS = 100;
    private static final int SETUP_TICKS = 0;

    private static final Identifier STRUCTURE =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "empty3x3x3");

    private MapArtGameTestRegistration() {
    }

    public static void register(RegisterGameTestsEvent event) {
        // vanilla の default 環境と同じ中身 (AllOf(空)) を自分の名前で登録し、その Holder を使う。
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Constants.MOD_ID, "default"),
                new TestEnvironmentDefinition.AllOf(List.of()));

        // block entity と地図データの振る舞い (1.21.1: MapArtGameTests)
        add(event, environment, "block_places_with_a_block_entity", MapArtGameTests::blockPlacesWithABlockEntity);
        add(event, environment, "stored_map_keeps_its_colours", MapArtGameTests::storedMapKeepsItsColours);
        add(event, environment, "blanks_are_consumed", MapArtGameTests::blanksAreConsumed);
        add(event, environment, "finished_map_is_locked", MapArtGameTests::finishedMapIsLocked);
        add(event, environment, "tiles_keep_their_position_in_the_grid", MapArtGameTests::tilesKeepTheirPositionInTheGrid);
        add(event, environment, "refuses_without_enough_blanks", MapArtGameTests::refusesWithoutEnoughBlanks);
        add(event, environment, "refuses_when_output_is_full", MapArtGameTests::refusesWhenOutputIsFull);
        add(event, environment, "uploaded_tiles_reassemble_and_store", MapArtGameTests::uploadedTilesReassembleAndStore);

        // 地図 ID の採番が検証より後であること (1.21.1: MapArtNetworkGameTests)
        add(event, environment, "no_block_path_mints_no_map_id", MapArtNetworkGameTests::noBlockPathMintsNoMapId);
        add(event, environment, "need_maps_path_mints_no_map_id", MapArtNetworkGameTests::needMapsPathMintsNoMapId);
        add(event, environment, "no_room_path_mints_no_map_id", MapArtNetworkGameTests::noRoomPathMintsNoMapId);
        add(event, environment, "dimension_mismatch_mints_no_map_id", MapArtNetworkGameTests::dimensionMismatchMintsNoMapId);
        add(event, environment, "successful_request_mints_exactly_the_tiles_it_stores",
                MapArtNetworkGameTests::successfulRequestMintsExactlyTheTilesItStores);
    }

    private static void add(RegisterGameTestsEvent event,
                            Holder<TestEnvironmentDefinition<?>> environment,
                            String name,
                            Consumer<GameTestHelper> body) {
        final TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(environment, STRUCTURE, MAX_TICKS, SETUP_TICKS, true);
        event.registerTest(Identifier.fromNamespaceAndPath(Constants.MOD_ID, name),
                new MapArtTestInstance(name, data, body));
    }
}
