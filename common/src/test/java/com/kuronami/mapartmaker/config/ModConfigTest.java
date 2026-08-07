package com.kuronami.mapartmaker.config;

import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigTest {

    @AfterEach
    void restoreDefaults() {
        ModConfig.apply(ModConfig.DEFAULT_MAX_TILES_PER_SIDE, ModConfig.DEFAULT_ALLOW_PRIVATE_HOSTS);
    }

    @Test
    void defaultsAreTheShippedBehaviour() {
        assertEquals(3, ModConfig.maxTilesPerSide());
        assertFalse(ModConfig.allowPrivateHosts(), "fetching from the server's own network stays off");
    }

    @Test
    void theCeilingMatchesTheOutputSlotCount() {
        // Allowing a bigger request than the block can hold would not make bigger art, it would
        // make every request fail the free-slot check in consumeBlanksAndStore.
        int ceiling = ModConfig.MAX_TILES_PER_SIDE_CEILING;
        assertTrue(ceiling * ceiling <= MapArtMakerBlockEntity.OUTPUT_SLOTS,
                "a full request must fit in the output slots: " + ceiling + "x" + ceiling
                        + " > " + MapArtMakerBlockEntity.OUTPUT_SLOTS);
    }

    @Test
    void tilesAreClampedIntoRange() {
        assertEquals(1, ModConfig.clampTiles(0));
        assertEquals(1, ModConfig.clampTiles(-5));
        assertEquals(1, ModConfig.clampTiles(1));
        assertEquals(2, ModConfig.clampTiles(2));
        assertEquals(3, ModConfig.clampTiles(3));
        assertEquals(3, ModConfig.clampTiles(9), "above the ceiling clamps down, never through");
    }

    @Test
    void applyTakesEffectAndStillClamps() {
        ModConfig.apply(1, true);
        assertEquals(1, ModConfig.maxTilesPerSide());
        assertTrue(ModConfig.allowPrivateHosts());

        ModConfig.apply(99, false);
        assertEquals(3, ModConfig.maxTilesPerSide());
        assertFalse(ModConfig.allowPrivateHosts());
    }
}
