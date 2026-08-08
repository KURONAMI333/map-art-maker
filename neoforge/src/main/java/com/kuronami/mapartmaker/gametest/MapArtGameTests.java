package com.kuronami.mapartmaker.gametest;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.mapart.MapArtService;
import com.kuronami.mapartmaker.register.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game behaviour that unit tests cannot reach: real world, real block entity, real map data.
 *
 * <p>The network layer is deliberately not involved. These drive the block entity directly, so a
 * failure points at world or map handling rather than at packet plumbing.
 */
@GameTestHolder(Constants.MOD_ID)
public class MapArtGameTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static MapArtMakerBlockEntity place(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MAP_ART_MAKER.get());
        if (!(helper.getBlockEntity(POS) instanceof MapArtMakerBlockEntity maker)) {
            helper.fail("no block entity after placing the block", POS);
            throw new IllegalStateException("unreachable");
        }
        return maker;
    }

    /** Distinct, in-range colour bytes so a mismatch cannot pass by landing on zeroes. */
    private static byte[] colours(int seed) {
        byte[] out = new byte[MapArtService.TILE * MapArtService.TILE];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) (4 + ((i + seed) % 200));
        }
        return out;
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void blockPlacesWithABlockEntity(GameTestHelper helper) {
        MapArtMakerBlockEntity maker = place(helper);
        if (maker.getContainerSize() != MapArtMakerBlockEntity.SIZE) {
            helper.fail("container size " + maker.getContainerSize()
                    + " does not match " + MapArtMakerBlockEntity.SIZE, POS);
        }
        if (!maker.isEmpty()) {
            helper.fail("a freshly placed block should be empty", POS);
        }
        helper.succeed();
    }

    /** The bytes handed in must survive into the map the player ends up holding. */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void storedMapKeepsItsColours(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MapArtMakerBlockEntity maker = place(helper);
        maker.setItem(MapArtMakerBlockEntity.SLOT_BLANK, new ItemStack(Items.MAP, 1));

        byte[] expected = colours(7);
        List<ItemStack> results = new ArrayList<>();
        results.add(MapArtService.createMap(level, expected));

        if (!maker.consumeBlanksAndStore(1, 1, results)) {
            helper.fail("storing one tile should succeed with one blank map available", POS);
        }

        ItemStack stored = maker.getItem(1);
        if (stored.isEmpty()) {
            helper.fail("output slot 1 is empty after a successful store", POS);
        }
        MapItemSavedData data = MapItem.getSavedData(stored, level);
        if (data == null) {
            helper.fail("the stored item carries no map data", POS);
        } else if (!java.util.Arrays.equals(expected, data.colors)) {
            helper.fail("map colours differ from what was written", POS);
        }
        helper.succeed();
    }

    /** The blank map has to be spent, exactly once per tile. */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void blanksAreConsumed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MapArtMakerBlockEntity maker = place(helper);
        maker.setItem(MapArtMakerBlockEntity.SLOT_BLANK, new ItemStack(Items.MAP, 4));

        List<ItemStack> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            results.add(MapArtService.createMap(level, colours(i)));
        }
        if (!maker.consumeBlanksAndStore(3, 1, results)) {
            helper.fail("a 3x1 strip should fit with four blanks and an empty grid", POS);
        }
        if (maker.blankCount() != 1) {
            helper.fail("expected one blank left, found " + maker.blankCount(), POS);
        }
        if (maker.freeOutputSlots() != MapArtMakerBlockEntity.OUTPUT_SLOTS - 3) {
            helper.fail("expected six free output slots, found " + maker.freeOutputSlots(), POS);
        }
        helper.succeed();
    }

    /** A finished map must never be repainted with real terrain, which only the lock prevents. */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void finishedMapIsLocked(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack map = MapArtService.createMap(level, colours(3));
        MapItemSavedData data = MapItem.getSavedData(map, level);
        if (data == null) {
            helper.fail("no map data on a freshly created tile", POS);
        } else if (!data.locked) {
            helper.fail("map art must be locked; MapItem's inventory tick repaints anything that is not", POS);
        }
        helper.succeed();
    }

    /** A 2x2 belongs in the top-left corner of the grid, not spread along the first four slots. */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void tilesKeepTheirPositionInTheGrid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MapArtMakerBlockEntity maker = place(helper);
        maker.setItem(MapArtMakerBlockEntity.SLOT_BLANK, new ItemStack(Items.MAP, 4));

        // Reading order: top-left, top-right, bottom-left, bottom-right.
        List<ItemStack> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            results.add(MapArtService.createMap(level, colours(i * 31)));
        }
        if (!maker.consumeBlanksAndStore(2, 2, results)) {
            helper.fail("a 2x2 should fit in an empty grid with four blanks", POS);
        }

        int[][] expected = {{0, 0}, {1, 0}, {0, 1}, {1, 1}};
        for (int i = 0; i < 4; i++) {
            int slot = MapArtMakerBlockEntity.outputSlot(expected[i][0], expected[i][1]);
            ItemStack stored = maker.getItem(slot);
            MapItemSavedData data = MapItem.getSavedData(stored, level);
            if (data == null || !java.util.Arrays.equals(colours(i * 31), data.colors)) {
                helper.fail("tile " + i + " is not in slot " + slot
                        + "; a 2x2 must fill the top-left corner of the grid", POS);
            }
        }
        // The right-hand column and bottom row belong to a 3x3 and must stay empty.
        for (int slot : new int[]{MapArtMakerBlockEntity.outputSlot(2, 0),
                MapArtMakerBlockEntity.outputSlot(2, 1), MapArtMakerBlockEntity.outputSlot(0, 2)}) {
            if (!maker.getItem(slot).isEmpty()) {
                helper.fail("slot " + slot + " is outside a 2x2 and should be empty", POS);
            }
        }
        helper.succeed();
    }

    /** Negative: without enough blanks nothing is produced and nothing is taken. */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void refusesWithoutEnoughBlanks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MapArtMakerBlockEntity maker = place(helper);
        maker.setItem(MapArtMakerBlockEntity.SLOT_BLANK, new ItemStack(Items.MAP, 1));

        List<ItemStack> results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            results.add(MapArtService.createMap(level, colours(i)));
        }
        if (maker.consumeBlanksAndStore(2, 2, results)) {
            helper.fail("a 2x2 must not be produced from a single blank map", POS);
        }
        if (maker.blankCount() != 1) {
            helper.fail("a refused request must not spend blanks, found " + maker.blankCount(), POS);
        }
        if (maker.freeOutputSlots() != MapArtMakerBlockEntity.OUTPUT_SLOTS) {
            helper.fail("a refused request must not fill output slots", POS);
        }
        helper.succeed();
    }

    /** Negative: a full output area blocks further work, which is what forces the player to empty it. */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void refusesWhenOutputIsFull(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MapArtMakerBlockEntity maker = place(helper);
        maker.setItem(MapArtMakerBlockEntity.SLOT_BLANK, new ItemStack(Items.MAP, 8));
        for (int slot = 1; slot < MapArtMakerBlockEntity.SIZE; slot++) {
            maker.setItem(slot, MapArtService.createMap(level, colours(slot)));
        }

        List<ItemStack> results = new ArrayList<>();
        results.add(MapArtService.createMap(level, colours(99)));
        if (maker.consumeBlanksAndStore(1, 1, results)) {
            helper.fail("a full output area must refuse further tiles", POS);
        }
        if (maker.blankCount() != 8) {
            helper.fail("a refused request must not spend blanks, found " + maker.blankCount(), POS);
        }
        helper.succeed();
    }
}
