package com.kuronami.mapartmaker.menu;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps the widgets off the slots.
 *
 * <p>The screen draws a URL field and a button row over the same panel the slots live in, and
 * nothing at runtime complains when they overlap — the widget simply covers the slot and the
 * player can no longer take the item. That is unrecoverable here: {@code
 * MapArtMakerBlockEntity.consumeBlanksAndStore} requires all nine output slots to be free, so a
 * covered slot means no further art can ever be made. These are plain rectangles derived from the
 * shared constants, so the check costs nothing and fails the moment a coordinate drifts.
 */
class MapArtMakerLayoutTest {

    /** Inclusive pixel rectangle. */
    private record Rect(String name, int x0, int y0, int x1, int y1) {
        boolean intersects(Rect other) {
            return x0 <= other.x1 && other.x0 <= x1 && y0 <= other.y1 && other.y0 <= y1;
        }
    }

    private static Rect widget(String name, int x, int y, int w, int h) {
        return new Rect(name, x, y, x + w - 1, y + h - 1);
    }

    /** Vanilla draws the 18x18 slot well one pixel out from the 16x16 content box. */
    private static Rect slotWell(String name, int contentX, int contentY) {
        return new Rect(name, contentX - 1, contentY - 1, contentX - 1 + 17, contentY - 1 + 17);
    }

    private static List<Rect> slots() {
        List<Rect> out = new ArrayList<>();
        out.add(slotWell("blank_map", MapArtMakerMenu.BLANK_SLOT_X, MapArtMakerMenu.BLANK_SLOT_Y));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                out.add(slotWell("output_" + row + column,
                        MapArtMakerMenu.OUTPUT_X + column * MapArtMakerMenu.SLOT_PITCH,
                        MapArtMakerMenu.OUTPUT_Y + row * MapArtMakerMenu.SLOT_PITCH));
            }
        }
        for (int row = 0; row < 3; row++) {
            out.add(slotWell("player_row" + row, 8,
                    MapArtMakerMenu.PANEL_HEIGHT - 82 + row * MapArtMakerMenu.SLOT_PITCH));
            out.add(slotWell("player_row" + row + "_end", 8 + 8 * MapArtMakerMenu.SLOT_PITCH,
                    MapArtMakerMenu.PANEL_HEIGHT - 82 + row * MapArtMakerMenu.SLOT_PITCH));
        }
        out.add(slotWell("hotbar", 8, MapArtMakerMenu.PANEL_HEIGHT - 24));
        out.add(slotWell("hotbar_end", 8 + 8 * MapArtMakerMenu.SLOT_PITCH,
                MapArtMakerMenu.PANEL_HEIGHT - 24));
        return out;
    }

    private static List<Rect> widgets() {
        List<Rect> out = new ArrayList<>();
        out.add(widget("url_box", MapArtMakerMenu.URL_BOX_X, MapArtMakerMenu.URL_BOX_Y,
                MapArtMakerMenu.URL_BOX_WIDTH, MapArtMakerMenu.URL_BOX_HEIGHT));
        out.add(widget("paste_button", MapArtMakerMenu.PASTE_BUTTON_X, MapArtMakerMenu.URL_BOX_Y,
                MapArtMakerMenu.PASTE_BUTTON_WIDTH, MapArtMakerMenu.URL_BOX_HEIGHT));
        int i = 0;
        for (int x : new int[]{8, 62, 116}) {
            out.add(widget("button_" + i++, x, MapArtMakerMenu.BUTTON_Y,
                    MapArtMakerMenu.BUTTON_WIDTH, MapArtMakerMenu.BUTTON_HEIGHT));
        }
        // The feedback line and the inventory label are drawn text, one font line tall.
        out.add(widget("feedback", 8, MapArtMakerMenu.FEEDBACK_Y, MapArtMakerMenu.URL_BOX_WIDTH, 9));
        out.add(widget("inventory_label", 8, MapArtMakerMenu.INVENTORY_LABEL_Y, 60, 9));
        return out;
    }

    @Test
    void noWidgetCoversASlot() {
        List<String> clashes = new ArrayList<>();
        for (Rect w : widgets()) {
            for (Rect s : slots()) {
                if (w.intersects(s)) {
                    clashes.add(w.name() + " covers " + s.name());
                }
            }
        }
        assertTrue(clashes.isEmpty(), () -> "widgets must not cover slots: " + clashes);
    }

    @Test
    void widgetsDoNotCoverEachOther() {
        List<Rect> all = widgets();
        List<String> clashes = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                if (all.get(i).intersects(all.get(j))) {
                    clashes.add(all.get(i).name() + " overlaps " + all.get(j).name());
                }
            }
        }
        assertTrue(clashes.isEmpty(), () -> "widgets must not overlap: " + clashes);
    }

    @Test
    void everythingFitsInsideThePanel() {
        List<Rect> all = new ArrayList<>(widgets());
        all.addAll(slots());
        List<String> outside = new ArrayList<>();
        for (Rect r : all) {
            if (r.x0() < 1 || r.y0() < 1
                    || r.x1() > MapArtMakerMenu.PANEL_WIDTH - 2
                    || r.y1() > MapArtMakerMenu.PANEL_HEIGHT - 2) {
                outside.add(r.name());
            }
        }
        assertTrue(outside.isEmpty(), () -> "must stay inside the panel border: " + outside);
    }
}
