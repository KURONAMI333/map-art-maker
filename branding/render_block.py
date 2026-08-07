"""Map Art Maker block faces - concept B, second pass.

Nothing here copies a Minecraft file. Vanilla was measured, not loaded: spruce_planks gave the
plank idiom (4px bands, the last row of each band a dark seam, a 3-4 step ramp inside the band)
and cartography_table gave the composition rule that actually matters - vanilla keeps its map
motif down to ~25 accent pixels and lets wood own the face. The first pass ignored that, filled
the whole top with 4px colour cells, and read as a sweet-shop grid instead of one picture.

So: our own plank palette, and a small inset picture that stays continuous across its seams. The
seams say "this is tiled"; the picture staying whole across them says "the tiles are one image".
"""

import os
import random

from PIL import Image

OUT = os.path.dirname(os.path.abspath(__file__))

# Our own wood, warm and mid-dark: clear of dark_oak (cartography already owns it) and of spruce.
PLANK_LIGHT = (140, 106, 64)
PLANK_MID = (129, 97, 57)
PLANK_LOW = (120, 90, 53)
PLANK_SEAM = (86, 62, 35)

# Map colours, the same numbers the block itself writes (MapColor at NORMAL brightness).
WATER = (55, 55, 190)
WATER_DEEP = (44, 44, 152)
LAND = (72, 108, 34)
LAND_HI = (96, 134, 44)
SHORE = (196, 180, 120)

FRAME = (74, 53, 30)
FRAME_HI = (152, 116, 70)

# Rolled blank maps: paper, not the map colours - nothing is printed on them yet.
PAPER = (196, 184, 148)
PAPER_HI = (218, 208, 176)
PAPER_EDGE = (150, 138, 108)


def planks(seed, breaks=(6,)):
    """4px bands, dark seam on the band's last row, plus a butt joint in one band.

    Each band gets its own slight tone offset and a couple of grain streaks; uniform bands read as
    corduroy, which is the giveaway of wood drawn by formula rather than observed.
    """
    rng = random.Random(seed)
    img = Image.new("RGB", (16, 16))
    px = img.load()
    band_tone = [PLANK_LIGHT, PLANK_MID, PLANK_LOW, PLANK_SEAM]
    for band in range(4):
        offset = rng.choice((-7, -3, 0, 3, 6))
        streaks = {rng.randrange(16) for _ in range(3)}
        for row in range(4):
            y = band * 4 + row
            tone = band_tone[row]
            for x in range(16):
                d = rng.randint(-4, 4) + (0 if row == 3 else offset)
                if x in streaks and row != 3:
                    d -= 9
                px[x, y] = tuple(max(0, min(255, c + d)) for c in tone)
    for bx in breaks:  # a butt joint between two planks, as vanilla does in one band
        for y in range(4, 8):
            px[bx, y] = tuple(max(0, c - 30) for c in px[bx, y])
    return img


def _scene():
    """An abstract landmass on water - the shape vanilla uses on its own map icons."""
    land = {
        (3, 4),
        (4, 4),
        (5, 4),
        (2, 5),
        (3, 5),
        (4, 5),
        (5, 5),
        (6, 5),
        (2, 6),
        (3, 6),
        (4, 6),
        (5, 6),
        (6, 6),
        (7, 6),
        (3, 7),
        (4, 7),
        (5, 7),
        (6, 7),
        (7, 7),
        (8, 7),
        (4, 8),
        (5, 8),
        (6, 8),
        (7, 8),
        (5, 9),
        (6, 9),
    }
    shore = {(6, 4), (7, 5), (8, 6), (9, 7), (8, 8), (7, 9), (4, 9)}
    return land, shore


def top_face(seams, size=12, seed=21):
    """Wood owns the face; a 12x12 inset holds the picture. Seams darken what is under them."""
    img = planks(seed)
    px = img.load()
    land, shore = _scene()
    x0 = y0 = (16 - size) // 2
    scale = 12 / size
    for y in range(size):
        for x in range(size):
            sx, sy = int(x * scale), int(y * scale)
            if (sx, sy) in land:
                c = LAND_HI if (sx + sy) % 5 == 0 else LAND
            elif (sx, sy) in shore:
                c = SHORE
            else:
                c = WATER_DEEP if (sx + sy) % 7 == 0 else WATER
            px[x0 + x, y0 + y] = c
    if seams:
        for i in range(size):
            for s in (size // 3, 2 * size // 3):
                for sx, sy in ((x0 + s, y0 + i), (x0 + i, y0 + s)):
                    px[sx, sy] = tuple(max(0, c - 46) for c in px[sx, sy])
    lo, hi = x0 - 1, x0 + size  # recessed frame, catch light on the top-left lip
    for i in range(lo, hi + 1):
        px[i, lo] = FRAME_HI
        px[lo, i] = FRAME_HI
        px[i, hi] = FRAME
        px[hi, i] = FRAME
    return img


def front_face(seed=22):
    """A rack of rolled blank maps - the stock this bench eats.

    The earlier draft put a finished map half way out of a mouth in this face, which the block does
    not have: results leave through the screen or a hopper underneath, never the front. Blanks are
    the one thing the front can honestly show, since the block really does hold a stack of them.
    """
    img = planks(seed, breaks=(11,))
    px = img.load()
    for y in range(8, 14):  # the rack well
        for x in range(2, 14):
            px[x, y] = FRAME
    for i in range(4):  # four rolled blanks, end on
        x0 = 3 + i * 3
        for y in range(9, 13):
            px[x0, y] = PAPER_EDGE
            px[x0 + 1, y] = PAPER
        px[x0, 9] = PAPER_HI
        px[x0 + 1, 9] = PAPER_HI
        px[x0, 12] = PAPER_EDGE
        px[x0 + 1, 12] = PAPER_EDGE
    for x in range(2, 14):  # lip above the rack
        px[x, 7] = FRAME_HI
    return img


def side_face(seed=23):
    """A bench flank: planks with an apron rail. Nothing to read - the top and front carry it."""
    img = planks(seed, breaks=(9,))
    px = img.load()
    for x in range(16):
        px[x, 4] = tuple(max(0, c - 28) for c in px[x, 4])
        px[x, 5] = FRAME_HI
    return img


def bottom_face(seed=24):
    return planks(seed, breaks=(3,))


CANDIDATES = {
    "d1_seamed": dict(seams=True, size=12),
    "d2_seamless": dict(seams=False, size=12),
    "d3_small": dict(seams=True, size=10),
}

if __name__ == "__main__":
    for name, cfg in CANDIDATES.items():
        d = os.path.join(OUT, "cand2", name)
        os.makedirs(d, exist_ok=True)
        top_face(cfg["seams"], cfg["size"]).save(os.path.join(d, "top.png"))
        front_face().save(os.path.join(d, "front.png"))
        side_face().save(os.path.join(d, "side.png"))
        bottom_face().save(os.path.join(d, "bottom.png"))
        print("wrote", d)
