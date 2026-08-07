package com.kuronami.mapartmaker.mapart;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns quantised pixels into vanilla filled maps. Server side only — map ids are world state.
 */
public final class MapArtService {

    /** Vanilla map canvas, from {@code MapItem.IMAGE_WIDTH}/{@code IMAGE_HEIGHT}. */
    public static final int TILE = 128;

    private MapArtService() {
    }

    /**
     * Splits an image across a tile grid and returns one filled map per tile, reading order.
     *
     * @param pixels ARGB pixels already scaled to {@code tilesX*128} by {@code tilesY*128}
     */
    public static List<ItemStack> createTiles(ServerLevel level, int[] pixels,
                                              int tilesX, int tilesY, boolean dither) {
        if (tilesX <= 0 || tilesY <= 0) {
            throw new IllegalArgumentException("tile counts must be positive");
        }
        int width = tilesX * TILE;
        int height = tilesY * TILE;
        if (pixels.length != width * height) {
            throw new IllegalArgumentException(
                    "expected " + (width * height) + " pixels for " + tilesX + "x" + tilesY + " tiles, got " + pixels.length);
        }

        ImageQuantizer quantizer = MapPalette.quantizer();
        List<ItemStack> stacks = new ArrayList<>(tilesX * tilesY);
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                int[] tile = crop(pixels, width, tx * TILE, ty * TILE);
                byte[] colours = quantizer.quantize(tile, TILE, TILE, dither);
                stacks.add(createMap(level, colours));
            }
        }
        return stacks;
    }

    /** Registers a new map in the world and returns the item holding it. */
    public static ItemStack createMap(ServerLevel level, byte[] colours) {
        if (colours.length != TILE * TILE) {
            throw new IllegalArgumentException("expected " + (TILE * TILE) + " colour bytes, got " + colours.length);
        }

        // trackingPosition/unlimitedTracking off: this is a picture, not a survey of the terrain,
        // so nothing should ever redraw it from world data.
        MapItemSavedData data = MapItemSavedData.createFresh(
                0.0D, 0.0D, (byte) 0, false, false, level.dimension());
        System.arraycopy(colours, 0, data.colors, 0, colours.length);
        data.setDirty();

        MapId id = level.getFreeMapId();
        level.setMapData(id, data);

        ItemStack stack = new ItemStack(Items.FILLED_MAP);
        stack.set(DataComponents.MAP_ID, id);
        return stack;
    }

    private static int[] crop(int[] pixels, int sourceWidth, int originX, int originY) {
        int[] out = new int[TILE * TILE];
        for (int y = 0; y < TILE; y++) {
            System.arraycopy(pixels, (originY + y) * sourceWidth + originX, out, y * TILE, TILE);
        }
        return out;
    }
}
