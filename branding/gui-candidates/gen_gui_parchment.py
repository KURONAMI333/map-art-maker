"""Preview: the cartography table's parchment idiom applied to our output grid.

Measured from vanilla's map preview sprite rather than copied: 66x66, three colours
(fill 214,190,150 / mark 153,135,108 / border 55,55,55), a dark outline that steps in and out by a
pixel at irregular intervals, gaps at the corners, and a few small marks just inside the edge. The
irregularity is the point - a border that wobbles on a fixed period reads as a pattern, not torn
paper.

Two variants, because the trade-off is kura's call:
  g1  parchment as a sheet behind the grid, slots keep the usual grey wells
  g2  parchment runs through the cells too, so the nine read as one sheet ruled into nine
"""

import os
import random
import sys

from PIL import Image

sys.path.insert(
    0, r"C:/Users/naoki/dev/projects/minecraft-mod-dev/mod-066-map-art-maker/branding"
)
import render_gui as base  # noqa: E402

OUT = os.path.dirname(os.path.abspath(__file__))

PARCHMENT = (214, 190, 150, 255)
PARCHMENT_MARK = (153, 135, 108, 255)
PARCHMENT_EDGE = (55, 55, 55, 255)

# Centred on the 3x3 output wells (x97..150, y73..126) with a small margin.
SHEET_X, SHEET_Y, SHEET_SIZE = 90, 66, 68


def parchment(img, seed=5):
    """A torn sheet: solid fill, then an outline whose thickness wanders by a pixel."""
    rng = random.Random(seed)
    px = img.load()
    x0, y0, n = SHEET_X, SHEET_Y, SHEET_SIZE
    for y in range(y0, y0 + n):
        for x in range(x0, x0 + n):
            px[x, y] = PARCHMENT

    # Per-side inset: mostly 0, sometimes 1, in runs rather than alternating pixel by pixel.
    def wander():
        out = []
        while len(out) < n:
            out.extend([rng.choice((0, 0, 0, 1))] * rng.randint(2, 6))
        return out[:n]

    top, bottom, left, right = wander(), wander(), wander(), wander()
    for i in range(n):
        for d in range(top[i] + 1):
            px[x0 + i, y0 + d] = PARCHMENT_EDGE
        for d in range(bottom[i] + 1):
            px[x0 + i, y0 + n - 1 - d] = PARCHMENT_EDGE
        for d in range(left[i] + 1):
            px[x0 + d, y0 + i] = PARCHMENT_EDGE
        for d in range(right[i] + 1):
            px[x0 + n - 1 - d, y0 + i] = PARCHMENT_EDGE

    # Corners come away, the way a sheet lifts off the surface underneath.
    for cx, cy in ((0, 0), (n - 1, 0), (0, n - 1), (n - 1, n - 1)):
        for dx in range(rng.randint(1, 3)):
            for dy in range(rng.randint(1, 3)):
                px[x0 + cx + (-dx if cx else dx), y0 + cy + (-dy if cy else dy)] = (
                    0,
                    0,
                    0,
                    0,
                )

    # A handful of nicks just inside the edge. Positions are drawn, not spaced.
    for _ in range(14):
        side = rng.randrange(4)
        along = rng.randrange(3, n - 3)
        depth = rng.randint(1, 2)
        if side == 0:
            spot = (x0 + along, y0 + 1 + depth)
        elif side == 1:
            spot = (x0 + along, y0 + n - 2 - depth)
        elif side == 2:
            spot = (x0 + 1 + depth, y0 + along)
        else:
            spot = (x0 + n - 2 - depth, y0 + along)
        px[spot] = PARCHMENT_MARK


def grid_cells(img, parchment_fill):
    """The nine cells. Borders always; the interior is parchment or the usual grey."""
    px = img.load()
    for row in range(3):
        for column in range(3):
            cx = base.OUTPUT_ORIGIN[0] + column * base.SLOT_PITCH - 1
            cy = base.OUTPUT_ORIGIN[1] + row * base.SLOT_PITCH - 1
            for i in range(17):
                px[cx + i, cy] = base.SLOT_EDGE
                px[cx, cy + i] = base.SLOT_EDGE
            if not parchment_fill:
                for y in range(1, 17):
                    for x in range(1, 17):
                        px[cx + x, cy + y] = base.SLOT_FILL
                for i in range(1, 18):
                    px[cx + 17, cy + i] = base.LIP_HI
                    px[cx + i, cy + 17] = base.LIP_HI
                px[cx + 17, cy] = base.SLOT_FILL
                px[cx, cy + 17] = base.SLOT_FILL


def build(parchment_fill):
    img = Image.new("RGBA", (base.SHEET, base.SHEET), (0, 0, 0, 0))
    base.panel(img)
    base.slot(img, *base.BLANK_SLOT)
    base.arrow(img, 55, 92)
    parchment(img)
    grid_cells(img, parchment_fill)
    for row in range(3):
        for column in range(9):
            base.slot(
                img,
                8 + column * base.SLOT_PITCH,
                base.PLAYER_INVENTORY_Y + row * base.SLOT_PITCH,
            )
    for column in range(9):
        base.slot(img, 8 + column * base.SLOT_PITCH, base.HOTBAR_Y)
    return img


if __name__ == "__main__":
    build(True).save(os.path.join(OUT, "gui_g3.png"))
    print("wrote gui_g3.png (parchment cells, roomier sheet)")
