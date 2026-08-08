# -*- coding: utf-8 -*-
"""Map Art Maker icon candidates — LOGO_PLAYBOOK classification (1) Subject-Max.

Not selected yet. kura picks from the contact sheet; final.py gets written
after that (see LOGO_PLAYBOOK gate: candidates -> sheet -> kura chooses ->
final).

Palette is sampled directly from the kura-approved block textures (no
invented colors) so every candidate shares one material vocabulary:
  branding/block-candidates/d1_seamed/top.png   (wood frame + blue lake +
                                                    green island, rows2-13
                                                    cols2-13 = the painting)
  branding/front-candidates/f1_drawer/front.png (drawer wood tones)

Two candidates depict the block's own finished map art (different frames,
different silhouettes so they aren't a recolor of the same family):
  A "framed"  - the block's own painting, cropped out of its bevel and
                re-mounted in a fresh frame (the mod-054 pinboard recipe:
                extract the board face, discard the surrounding wood). A
                coastline pass darkens water pixels that touch land so the
                island reads as a shape, not a green smear. Flat dark-wood
                background.
  B "easel"   - a twin-peak mountain ridge (fresh art, not reused pixels) on
                a canvas standing on painter's-easel legs. The ridge is
                built from an explicit per-column skyline instead of a
                smooth triangle so it has a jagged, recognizable profile,
                plus the same coastline pass at the shoreline. Flat
                parchment-tan background.

One candidate depicts the conversion itself — an external picture becoming
a Minecraft map — instead of another finished landscape in another frame:
  D "convert" - a plain, seamless photo tile on top, a bold arrow, and a
                3x3 grid of colored map tiles on the bottom (the block's own
                seamed-tile idiom, reused as the composition's language
                since it's already how this MOD's output actually looks).
                No frame, transparent background.

Outputs:
  branding/icon-candidates/mam_A_framed_512/256/128/64.png
  branding/icon-candidates/mam_B_easel_512/256/128/64.png
  branding/icon-candidates/mam_D_convert_512/256/128/64.png
  branding/icon-candidates/_contact_sheet.png   (96px + 48px insets)
"""

import os

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(__file__)
OUT_DIR = os.path.join(HERE, "icon-candidates")
os.makedirs(OUT_DIR, exist_ok=True)

SS, OUT = 4, 512
N = OUT * SS  # 2048 supersample canvas

# ---------------------------------------------------------------------------
# Palette — sampled from block-candidates/d1_seamed/top.png and
# front-candidates/f1_drawer/front.png (see module docstring). No new hues.
# ---------------------------------------------------------------------------
WOOD_LIGHT = (152, 116, 70)
WOOD_MED = (140, 106, 64)
WOOD_DARK = (74, 53, 30)
WOOD_DARKEST = (89, 65, 38)

BLUE = (55, 55, 190)
BLUE_MID = (44, 44, 152)
BLUE_DARK = (9, 9, 144)
BLUE_DARKEST = (0, 0, 106)

GREEN = (72, 108, 34)
GREEN_LIGHT = (96, 134, 44)
GREEN_DARK = (26, 62, 0)

SAND = (150, 134, 74)
SAND_LIGHT = (196, 180, 120)

WATER_SET = {BLUE, BLUE_MID, BLUE_DARK, BLUE_DARKEST}
LAND_SET = {GREEN, GREEN_LIGHT, GREEN_DARK, SAND, SAND_LIGHT}


def rgba(rgb: tuple[int, int, int], a: int = 255) -> tuple[int, int, int, int]:
    return (rgb[0], rgb[1], rgb[2], a)


def rounded_bg(
    rgb: tuple[int, int, int], size: int = N, radius_frac: float = 0.20
) -> Image.Image:
    big = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    radius = int(size * radius_frac)
    d.rounded_rectangle([0, 0, size, size], radius=radius, fill=rgba(rgb))
    return big


def grid_to_array(grid: list[list[tuple[int, ...] | None]]) -> np.ndarray:
    """grid: list of rows of RGBA tuples or None (transparent) -> HxWx4 uint8."""
    h = len(grid)
    w = len(grid[0])
    arr = np.zeros((h, w, 4), dtype=np.uint8)
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            if c is not None:
                arr[y, x] = c
    return arr


def add_coastline(
    grid_rgb: list[list[tuple[int, int, int]]],
    dark_water: tuple[int, int, int] = BLUE_DARKEST,
) -> list[list[tuple[int, int, int]]]:
    """Darken water pixels that 4-touch a land pixel. Vanilla map rendering
    reads as terrain mostly from this land/water contrast line, not from
    the fill color, so a plain filled-in blob needs this pass to read as a
    shape rather than a smear. Returns a new grid (input is not mutated)."""
    h = len(grid_rgb)
    w = len(grid_rgb[0])
    out = [row[:] for row in grid_rgb]
    for y in range(h):
        for x in range(w):
            if grid_rgb[y][x] not in WATER_SET:
                continue
            neighbors = []
            if y > 0:
                neighbors.append(grid_rgb[y - 1][x])
            if y < h - 1:
                neighbors.append(grid_rgb[y + 1][x])
            if x > 0:
                neighbors.append(grid_rgb[y][x - 1])
            if x < w - 1:
                neighbors.append(grid_rgb[y][x + 1])
            if any(nb in LAND_SET for nb in neighbors):
                out[y][x] = dark_water
    return out


def paste_grid_nearest(
    canvas: Image.Image,
    grid: list[list[tuple[int, ...] | None]],
    cx: float,
    cy: float,
    scale: int,
    anchor: str = "center",
) -> tuple[int, int, int, int]:
    """NEAREST-scale a pixel grid by an integer factor and paste centered on
    (cx, cy) in canvas coords. Returns the pasted bbox (x0, y0, x1, y1)."""
    arr = grid_to_array(grid)
    gh, gw = arr.shape[0], arr.shape[1]
    im = Image.fromarray(arr, mode="RGBA").resize(
        (gw * scale, gh * scale), Image.NEAREST
    )
    w, h = im.size
    if anchor == "center":
        x0, y0 = int(cx - w / 2), int(cy - h / 2)
    elif anchor == "bottom-center":
        x0, y0 = int(cx - w / 2), int(cy - h)
    else:
        raise ValueError(anchor)
    canvas.alpha_composite(im, (x0, y0))
    return x0, y0, x0 + w, y0 + h


# ---------------------------------------------------------------------------
# Candidate A — "framed": the block's own island painting, cropped out of
# its original bevel, re-mounted in a fresh frame, coastline-enhanced.
# ---------------------------------------------------------------------------
def build_candidate_a() -> Image.Image:
    top_path = os.path.join(HERE, "block-candidates", "d1_seamed", "top.png")
    top = Image.open(top_path).convert("RGBA")
    # rows/cols 3..12 (10x10): the island and its immediate water, with one
    # less ring of plain lake padding than a full-face crop so the island
    # is a bigger share of the subject.
    painting = top.crop((3, 3, 13, 13))
    pw, ph = painting.size  # 10,10

    paint_rgb = [
        [tuple(painting.getpixel((x, y))[:3]) for x in range(pw)] for y in range(ph)
    ]
    paint_rgb = add_coastline(paint_rgb)
    paint_rgba = [[c + (255,) for c in row] for row in paint_rgb]

    FRAME = 1  # px frame on each side, fresh 2-tone bevel
    fw, fh = pw + FRAME * 2, ph + FRAME * 2  # 12,12

    canvas = rounded_bg(WOOD_DARK)

    subject_frac = 0.82
    scale = int(round((N * subject_frac) / fw))
    cx = cy = N / 2

    frame_grid = [[WOOD_LIGHT + (255,) for _ in range(fw)] for _ in range(fh)]
    for x in range(fw):
        frame_grid[fh - 1][x] = WOOD_DARK + (255,)
    for y in range(fh):
        frame_grid[y][fw - 1] = WOOD_DARK + (255,)
    x0, y0, _, _ = paste_grid_nearest(canvas, frame_grid, cx, cy, scale)

    paint_im = Image.fromarray(grid_to_array(paint_rgba), mode="RGBA").resize(
        (pw * scale, ph * scale), Image.NEAREST
    )
    canvas.alpha_composite(paint_im, (x0 + FRAME * scale, y0 + FRAME * scale))

    return canvas


# ---------------------------------------------------------------------------
# Candidate B — "easel": a twin-peak mountain ridge (fresh art, not reused
# pixels) on a canvas standing on painter's-easel legs.
# ---------------------------------------------------------------------------
def build_candidate_b() -> Image.Image:
    canvas = rounded_bg(SAND_LIGHT)
    cw, ch = 12, 9  # canvas interior grid

    # explicit per-column skyline -> a jagged twin-peak ridge, not a smooth
    # triangle. Lower value = taller peak at that column.
    heights = [8, 7, 6, 4, 2, 3, 5, 3, 2, 4, 6, 8]
    sky_and_ridge: list[list[tuple[int, int, int]]] = [
        [BLUE for _ in range(cw)] for _ in range(ch - 1)
    ]
    for x in range(cw):
        for y in range(ch - 1):
            if y >= heights[x]:
                sky_and_ridge[y][x] = GREEN
            else:
                sky_and_ridge[y][x] = BLUE_MID if y >= 4 else BLUE
    # snow caps at the two peaks
    for x in (4, 8):
        sky_and_ridge[heights[x]][x] = SAND_LIGHT
    # sun, clear of both peaks
    for y in (0, 1):
        for x in (10, 11):
            sky_and_ridge[y][x] = SAND_LIGHT

    # waterline: lake at the mountain's base, full width
    grid_rgb = sky_and_ridge + [[BLUE_DARK for _ in range(cw)]]
    grid_rgb = add_coastline(grid_rgb)

    grid_rgba = [[c + (255,) for c in row] for row in grid_rgb]

    fw, fh = cw + 2, ch + 2  # 1px frame all around
    frame_grid = [[WOOD_MED + (255,) for _ in range(fw)] for _ in range(fh)]
    for x in range(fw):
        frame_grid[0][x] = WOOD_LIGHT + (255,)
        frame_grid[fh - 1][x] = WOOD_DARK + (255,)
    for y in range(fh):
        frame_grid[y][0] = WOOD_LIGHT + (255,)
        frame_grid[y][fw - 1] = WOOD_DARK + (255,)

    subject_frac = 0.60  # canvas alone; legs extend the bbox further down
    scale = int(round((N * subject_frac) / fw))
    cx = N / 2
    cy = N * 0.42  # bias upward, legs occupy the lower third

    x0, y0, x1, y1 = paste_grid_nearest(canvas, frame_grid, cx, cy, scale)
    inner_im = Image.fromarray(grid_to_array(grid_rgba), mode="RGBA").resize(
        (cw * scale, ch * scale), Image.NEAREST
    )
    canvas.alpha_composite(inner_im, (x0 + scale, y0 + scale))

    # easel legs: vector polygons in WOOD_DARK, splayed from the canvas base
    d = ImageDraw.Draw(canvas)
    leg_w = scale * 0.9
    base_y = y1
    foot_y = N * 0.94
    left_top = (x0 + fw * scale * 0.18, base_y)
    left_foot = (N * 0.16, foot_y)
    right_top = (x0 + fw * scale * 0.82, base_y)
    right_foot = (N * 0.84, foot_y)
    for top_pt, foot_pt in ((left_top, left_foot), (right_top, right_foot)):
        dx = foot_pt[0] - top_pt[0]
        dy = foot_pt[1] - top_pt[1]
        length = max((dx**2 + dy**2) ** 0.5, 1.0)
        nx, ny = -dy / length * leg_w / 2, dx / length * leg_w / 2
        poly = [
            (top_pt[0] + nx, top_pt[1] + ny),
            (foot_pt[0] + nx, foot_pt[1] + ny),
            (foot_pt[0] - nx, foot_pt[1] - ny),
            (top_pt[0] - nx, top_pt[1] - ny),
        ]
        d.polygon(poly, fill=rgba(WOOD_DARK))
    d.line(
        [(N / 2, base_y), (N / 2, foot_y * 0.86)],
        fill=rgba(WOOD_DARK),
        width=int(leg_w * 0.7),
    )

    return canvas


# ---------------------------------------------------------------------------
# Candidate D — "convert": the mechanic itself. A plain photo tile becomes
# a 3x3 grid of map tiles (the block's own seamed-tile idiom). No frame.
# ---------------------------------------------------------------------------
def build_candidate_d() -> Image.Image:
    canvas = Image.new("RGBA", (N, N), (0, 0, 0, 0))

    tile_w = 17  # shared width for both tiles

    # --- photo tile: 1px border, flat sky/ground bands, no internal seams
    photo_h = 7
    photo = [[SAND_LIGHT for _ in range(tile_w)] for _ in range(photo_h)]
    for y in range(1, photo_h - 1):
        for x in range(1, tile_w - 1):
            photo[y][x] = BLUE if y < 3 else GREEN
    for y in (1, 2):
        for x in (tile_w - 4, tile_w - 3):
            photo[y][x] = SAND_LIGHT  # sun, upper-right, inside the sky band

    # --- map tile: 3x3 cells (5x3 each), 1px seams -> 17x11, wider/shorter
    # cells than a square grid so the whole composition isn't a tall,
    # narrow strip lost in the middle of a square frame. Block's own
    # seamed-tile idiom, just with non-square cells.
    cell_w, cell_h, seam = 5, 3, 1
    map_w = cell_w * 3 + seam * 2
    map_h = cell_h * 3 + seam * 2
    cell_colors = [
        [BLUE, BLUE_MID, BLUE],
        [GREEN, SAND, GREEN_LIGHT],
        [GREEN_DARK, GREEN, BLUE_DARK],
    ]
    map_grid = [[WOOD_DARK for _ in range(map_w)] for _ in range(map_h)]
    for cr in range(3):
        for cc in range(3):
            y0 = cr * (cell_h + seam)
            x0 = cc * (cell_w + seam)
            for dy in range(cell_h):
                for dx in range(cell_w):
                    map_grid[y0 + dy][x0 + dx] = cell_colors[cr][cc]
    # tile_w == map_w by construction (both 17)

    photo_rgba = [[c + (255,) for c in row] for row in photo]
    map_rgba = [[c + (255,) for c in row] for row in map_grid]

    ARROW_GAP = 2
    total_h = photo_h + ARROW_GAP + map_h
    subject_frac = 0.86
    scale = int(round((N * subject_frac) / max(tile_w, total_h)))

    top_y = N / 2 - (total_h * scale) / 2
    left_x = N / 2 - (tile_w * scale) / 2

    photo_im = Image.fromarray(grid_to_array(photo_rgba), mode="RGBA").resize(
        (tile_w * scale, photo_h * scale), Image.NEAREST
    )
    canvas.alpha_composite(photo_im, (int(left_x), int(top_y)))

    map_left_x = N / 2 - (map_w * scale) / 2
    map_top_y = top_y + (photo_h + ARROW_GAP) * scale
    map_im = Image.fromarray(grid_to_array(map_rgba), mode="RGBA").resize(
        (map_w * scale, map_h * scale), Image.NEAREST
    )
    canvas.alpha_composite(map_im, (int(map_left_x), int(map_top_y)))

    # bold downward arrow between the two tiles
    d = ImageDraw.Draw(canvas)
    ax = N / 2
    ay0 = top_y + photo_h * scale + scale * 0.15
    ay1 = top_y + (photo_h + ARROW_GAP) * scale - scale * 0.15
    shaft_w = scale * 1.1
    head_w = scale * 2.4
    head_h = scale * 1.1
    d.line([(ax, ay0), (ax, ay1 - head_h)], fill=rgba(WOOD_DARK), width=int(shaft_w))
    d.polygon(
        [
            (ax - head_w / 2, ay1 - head_h),
            (ax + head_w / 2, ay1 - head_h),
            (ax, ay1),
        ],
        fill=rgba(WOOD_DARK),
    )

    return canvas


# ---------------------------------------------------------------------------
# Render + export
# ---------------------------------------------------------------------------
CANDIDATES = {
    "mam_A_framed": build_candidate_a,
    "mam_B_easel": build_candidate_b,
    "mam_D_convert": build_candidate_d,
}

masters: dict[str, Image.Image] = {}
for name, builder in CANDIDATES.items():
    ss_canvas = builder()
    master = ss_canvas.resize((OUT, OUT), Image.LANCZOS)
    masters[name] = master
    for sz in (512, 256, 128, 64):
        master.resize((sz, sz), Image.LANCZOS).save(
            os.path.join(OUT_DIR, f"{name}_{sz}.png")
        )
    print("saved", name, "512/256/128/64")

# ---------------------------------------------------------------------------
# Contact sheet: full 512 + 96px inset + 48px inset per candidate, labeled.
# ---------------------------------------------------------------------------
CELL = 220
PAD = 24
LABEL_H = 28
cols = len(masters)
sheet_w = cols * CELL + (cols + 1) * PAD
sheet_h = CELL + 96 + 48 + LABEL_H * 3 + PAD * 4
sheet = Image.new("RGBA", (sheet_w, sheet_h), (40, 40, 40, 255))
d = ImageDraw.Draw(sheet)
try:
    font = ImageFont.truetype("arial.ttf", 18)
except Exception:
    font = ImageFont.load_default()

x = PAD
for name, master in masters.items():
    big = master.resize((CELL, CELL), Image.LANCZOS)
    sheet.paste(big, (x, PAD), big)
    y = PAD + CELL + 6
    d.text((x, y), name, fill=(255, 255, 255, 255), font=font)
    y += LABEL_H
    inset96 = master.resize((96, 96), Image.LANCZOS)
    sheet.paste(inset96, (x, y), inset96)
    d.text((x + 104, y + 36), "96px", fill=(200, 200, 200, 255), font=font)
    y += 96 + LABEL_H
    inset48 = master.resize((48, 48), Image.LANCZOS)
    sheet.paste(inset48, (x, y), inset48)
    d.text((x + 56, y + 16), "48px", fill=(200, 200, 200, 255), font=font)
    x += CELL + PAD

sheet_path = os.path.join(OUT_DIR, "_contact_sheet.png")
sheet.convert("RGB").save(sheet_path)
print("saved", sheet_path)
