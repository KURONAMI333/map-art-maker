package com.kuronami.mapartmaker.gametest;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.mapart.MapArtService;
import com.kuronami.mapartmaker.network.ModNetwork;
import com.kuronami.mapartmaker.register.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Exercises {@link ModNetwork#storeAssembledMaps} directly across its rejection paths.
 *
 * <p>The regression this guards against: {@code storeAssembledMaps} used to receive an
 * already-built {@code List<ItemStack>}, which meant every map in it had already been minted (a
 * permanent {@code map_N.dat} entry, {@link ServerLevel#getFreeMapId()} advanced) by the time any
 * of {@code no_block}/{@code need_maps}/{@code no_room} could reject the request. Every negative
 * test below hands in a {@code mapSupplier} that would mint a map if it were ever invoked, and
 * checks two independent things: the supplier was never called, and the world's map id counter
 * did not move. Either one alone could pass by accident; both together cannot.
 */
@GameTestHolder(Constants.MOD_ID)
public class MapArtNetworkGameTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private static MapArtMakerBlockEntity place(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.MAP_ART_MAKER.get());
        if (!(helper.getBlockEntity(POS) instanceof MapArtMakerBlockEntity maker)) {
            helper.fail("no block entity after placing the block", POS);
            throw new IllegalStateException("unreachable");
        }
        return maker;
    }

    private static byte[] colours(int seed) {
        byte[] out = new byte[MapArtService.TILE * MapArtService.TILE];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) (4 + ((i + seed) % 200));
        }
        return out;
    }

    /** A dimension guaranteed to differ from wherever this GameTest happens to be running. */
    private static ResourceKey<Level> otherDimension(ServerLevel level) {
        return level.dimension().equals(Level.NETHER) ? Level.OVERWORLD : Level.NETHER;
    }

    /**
     * A {@code mapSupplier} that records whether it ran and, if it does run, actually mints a map
     * — so a caller that wrongly invokes it despite an earlier rejection is caught by the id-count
     * assertion too, not just the "was it called" flag.
     */
    private static java.util.function.Function<ServerLevel, List<ItemStack>> trackedSupplier(AtomicBoolean called) {
        return level -> {
            called.set(true);
            return List.of(MapArtService.createMap(level, colours(1)));
        };
    }

    /**
     * {@code storeAssembledMaps} always ends by sending a {@code MapArtFeedbackPayload}, success
     * or failure. {@link GameTestHelper#makeMockServerPlayerInLevel()} wires up a bare
     * {@code Connection} that never went through the real client handshake that negotiates known
     * payload channels, so that send throws here every time — after every check under test has
     * already run its course, since {@code send}/{@code fail} is always the last statement on its
     * branch. Swallowing it is a test-harness accommodation, not a change to what is being tested.
     */
    private static void invoke(ServerPlayer player, BlockPos pos, ResourceKey<Level> dimension,
                                int tilesX, int tilesY, java.util.function.Function<ServerLevel, List<ItemStack>> supplier) {
        try {
            ModNetwork.storeAssembledMaps(player, pos, dimension, tilesX, tilesY, supplier);
        } catch (RuntimeException e) {
            if (!thrownByFeedbackSend(e)) {
                throw e;
            }
        }
    }

    /**
     * Only the feedback send is excused. Swallowing every {@code RuntimeException} would let a
     * branch that blows up before reaching its decision pass the "nothing was minted" assertions
     * for the wrong reason, since an early throw leaves the supplier uncalled and the id counter
     * still.
     */
    private static boolean thrownByFeedbackSend(RuntimeException e) {
        for (StackTraceElement frame : e.getStackTrace()) {
            if (ModNetwork.class.getName().equals(frame.getClassName()) && "send".equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static void assertNoMapMinted(GameTestHelper helper, AtomicBoolean supplierCalled,
                                           int idCounterBefore, int idCounterAfter, String scenario) {
        if (supplierCalled.get()) {
            helper.fail(scenario + ": the map supplier ran despite the request being refused", POS);
        }
        // Our own two getFreeMapId() probes account for exactly +1; anything else means the
        // operation under test minted a map of its own.
        if (idCounterAfter != idCounterBefore + 1) {
            helper.fail(scenario + ": the map id counter moved by " + (idCounterAfter - idCounterBefore - 1)
                    + " beyond the test's own probes", POS);
        }
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void noBlockPathMintsNoMapId(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absPos = helper.absolutePos(POS);
        // Deliberately not placed: no block entity at absPos.

        AtomicBoolean called = new AtomicBoolean(false);
        int before = level.getFreeMapId().id();
        invoke(player, absPos, level.dimension(), 1, 1, trackedSupplier(called));
        int after = level.getFreeMapId().id();

        assertNoMapMinted(helper, called, before, after, "no_block");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void needMapsPathMintsNoMapId(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absPos = helper.absolutePos(POS);
        place(helper); // no blanks fed in: blankCount() is 0

        AtomicBoolean called = new AtomicBoolean(false);
        int before = level.getFreeMapId().id();
        invoke(player, absPos, level.dimension(), 1, 1, trackedSupplier(called));
        int after = level.getFreeMapId().id();

        assertNoMapMinted(helper, called, before, after, "need_maps");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void noRoomPathMintsNoMapId(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absPos = helper.absolutePos(POS);
        MapArtMakerBlockEntity maker = place(helper);
        maker.setItem(MapArtMakerBlockEntity.SLOT_BLANK, new ItemStack(Items.MAP, 1));
        // Blanks are plentiful but the only output slot a 1x1 could land in is already occupied.
        maker.setItem(MapArtMakerBlockEntity.outputSlot(0, 0), MapArtService.createMap(level, colours(9)));

        AtomicBoolean called = new AtomicBoolean(false);
        int before = level.getFreeMapId().id();
        invoke(player, absPos, level.dimension(), 1, 1, trackedSupplier(called));
        int after = level.getFreeMapId().id();

        assertNoMapMinted(helper, called, before, after, "no_room");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void dimensionMismatchMintsNoMapId(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absPos = helper.absolutePos(POS);
        MapArtMakerBlockEntity maker = place(helper);
        maker.setItem(MapArtMakerBlockEntity.SLOT_BLANK, new ItemStack(Items.MAP, 1));
        // Everything else about this request would succeed; only the recorded dimension is wrong.

        AtomicBoolean called = new AtomicBoolean(false);
        int before = level.getFreeMapId().id();
        invoke(player, absPos, otherDimension(level), 1, 1, trackedSupplier(called));
        int after = level.getFreeMapId().id();

        assertNoMapMinted(helper, called, before, after, "dimension_mismatch");
        helper.succeed();
    }

    /** Positive control: the deferred-creation refactor must not have broken the happy path. */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty3x3x3")
    public static void successfulRequestMintsExactlyTheTilesItStores(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos absPos = helper.absolutePos(POS);
        MapArtMakerBlockEntity maker = place(helper);
        maker.setItem(MapArtMakerBlockEntity.SLOT_BLANK, new ItemStack(Items.MAP, 4));

        byte[][] expected = {colours(1), colours(2), colours(3), colours(4)};
        AtomicBoolean called = new AtomicBoolean(false);
        int before = level.getFreeMapId().id();
        invoke(player, absPos, level.dimension(), 2, 2, lvl -> {
            called.set(true);
            return MapArtService.createMaps(lvl, expected);
        });
        int after = level.getFreeMapId().id();

        if (!called.get()) {
            helper.fail("a request that should succeed never invoked the map supplier", POS);
        }
        // Our two probes (+1) plus the four tiles actually minted.
        if (after != before + 1 + 4) {
            helper.fail("expected exactly 4 maps minted, counter moved by " + (after - before - 1), POS);
        }
        if (maker.blankCount() != 0) {
            helper.fail("expected all four blanks consumed, found " + maker.blankCount(), POS);
        }
        helper.succeed();
    }
}
