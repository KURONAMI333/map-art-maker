"""Map Art Maker block faces - concept B, a bench where map tiles are assembled into one picture.

Nothing here copies a Minecraft file. Vanilla was measured, not loaded, and three things came out
of that measurement:

- the plank idiom: 4px bands, the last row of each band a dark seam, 3-4 steps inside a band
- the composition rule: vanilla keeps its map motif to ~25 accent pixels and lets wood own the face
- the palette rule: spruce_planks is 7 colours flat. One material stays in single digits

Every colour below comes from a fixed list and pixels are assigned to entries in it. Nothing is
computed per pixel: a random offset on each pixel produces a texture with ninety-odd colours that
reads as photographic noise rather than pixel art, which is what the first version did.
"""

import os
import random

from PIL import Image

OUT = os.path.dirname(os.path.abspath(__file__))

# Wood, light to dark. Warm and mid-dark: clear of the dark oak cartography already owns.
WOOD = [
    (146, 111, 67),
    (136, 103, 61),
    (127, 95, 56),
    (117, 88, 51),
    (86, 62, 35),
]
SEAM = 4  # index into WOOD for a band's closing row

# Map colours, the same numbers the block itself writes (MapColor at NORMAL brightness).
WATER = (55, 55, 190)
WATER_DEEP = (44, 44, 152)
LAND = (72, 108, 34)
LAND_HI = (96, 134, 44)
SHORE = (196, 180, 120)

FRAME = (74, 53, 30)
FRAME_HI = (152, 116, 70)


def planks(seed, breaks=(6,)):
    """4px bands with a dark closing row, a butt joint in one band, and a few grain streaks.

    Tone comes from picking a neighbouring index in WOOD, never from offsetting a channel, so the
    face stays inside a five colour wood palette however much it is roughened.
    """
    rng = random.Random(seed)
    img = Image.new("RGB", (16, 16))
    px = img.load()
    for band in range(4):
        lift = rng.choice((-1, 0, 0, 1))
        streaks = {rng.randrange(16) for _ in range(3)}
        for row in range(4):
            y = band * 4 + row
            for x in range(16):
                if row == 3:
                    index = SEAM
                else:
                    index = row + lift
                    if x in streaks:
                        index += 1
                    index += rng.choice((-1, 0, 0, 1))
                px[x, y] = WOOD[max(0, min(len(WOOD) - 1, index))]
    for bx in breaks:
        for y in range(4, 8):
            px[bx, y] = WOOD[SEAM]
    return img


def _scene():
    """An abstract landmass on water, the motif vanilla uses on its own map icons."""
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


def top_face(seed=21, size=12):
    """Wood owns the face; a 12x12 inset holds the picture, whole across its tile seams."""
    img = planks(seed)
    px = img.load()
    land, shore = _scene()
    x0 = y0 = (16 - size) // 2
    for y in range(size):
        for x in range(size):
            if (x, y) in land:
                # Light comes from the top left, so the coast facing it catches it. Scattering the
                # highlight on a modulus instead would put speckle everywhere and read as noise.
                lit = (x, y - 1) not in land or (x - 1, y) not in land
                c = LAND_HI if lit else LAND
            elif (x, y) in shore:
                c = SHORE
            else:
                shaded = (x, y - 1) in land or (x - 1, y) in land
                c = WATER_DEEP if shaded else WATER
            px[x0 + x, y0 + y] = c
    for i in range(size):  # the tiling shows, the picture does not break
        for s in (size // 3, 2 * size // 3):
            for sx, sy in ((x0 + s, y0 + i), (x0 + i, y0 + s)):
                px[sx, sy] = WATER_DEEP if px[sx, sy] in (WATER, WATER_DEEP) else FRAME
    lo, hi = x0 - 1, x0 + size
    for i in range(lo, hi + 1):
        px[i, lo] = FRAME_HI
        px[lo, i] = FRAME_HI
        px[i, hi] = FRAME
        px[hi, i] = FRAME
    return img


def front_face(seed=22):
    """One wide drawer. Furniture, not props - the top face already carries the identity."""
    img = planks(seed, breaks=(11,))
    px = img.load()
    for y in range(9, 13):
        for x in range(3, 13):
            px[x, y] = WOOD[3]
    for x in range(2, 14):
        px[x, 8] = FRAME
        px[x, 13] = FRAME_HI
    for y in range(8, 14):
        px[2, y] = FRAME
        px[13, y] = FRAME
    for x in range(7, 11):  # a single pull, slightly right of centre
        px[x, 10] = FRAME
        px[x, 11] = FRAME_HI
    return img


def side_face(seed=23):
    """A bench flank: planks with an apron rail. Nothing to read - the top and front carry it."""
    img = planks(seed, breaks=(9,))
    px = img.load()
    for x in range(16):
        px[x, 4] = FRAME
        px[x, 5] = FRAME_HI
    return img


def bottom_face(seed=24):
    return planks(seed, breaks=(3,))


def write_faces(target):
    os.makedirs(target, exist_ok=True)
    faces = {
        "map_art_maker_top": top_face(),
        "map_art_maker_front": front_face(),
        "map_art_maker_side": side_face(),
        "map_art_maker_bottom": bottom_face(),
    }
    for name, img in faces.items():
        path = os.path.join(target, name + ".png")
        img.save(path)
        print("wrote", path)


if __name__ == "__main__":
    import sys

    default = os.path.join(
        os.path.dirname(OUT),
        "common",
        "src",
        "main",
        "resources",
        "assets",
        "map_art_maker",
        "textures",
        "block",
    )
    write_faces(sys.argv[1] if len(sys.argv) > 1 else default)
