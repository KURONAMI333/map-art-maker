# -*- coding: utf-8 -*-
"""Map Art Maker icon — FINAL. kura picked B "easel" (2026-08-08): a
twin-peak mountain ridge landscape on a canvas standing on painter's-easel
legs, flat parchment-tan background. kura's own comment on the pick: "少し
内容とはニュアンスが違うが、わかりやすさを優先" (the easel-painting metaphor
runs a little off the mod's literal mechanic, but it reads the clearest).
Per that framing, the artwork is not touched here — see
branding/icon-candidates/mam_B_easel_*_CONFIRMED_see_branding_root_for_final.png
for the selected candidate and DECISION.md (this directory) for the full
candidate round.

Palette is sampled from the kura-approved block textures (no invented
colors): branding/block-candidates/d1_seamed/top.png (wood + blue lake +
green island) and branding/front-candidates/f1_drawer/front.png (drawer
wood tones).

Outputs:
  branding/map_art_maker_icon_512/256/128/64.png   publish masters
  branding/logo_injar_256.png                       in-jar logo staging copy
  common/src/main/resources/map_art_maker.png       in-jar logo, wired
    (common's resources are merged into both the neoforge and fabric jars
    by the multiloader-loader convention plugin's commonResources config,
    confirmed against mod-062-welcome-board's common/src/main/resources/
    welcome_board.png -> both loader jars)
"""

import os

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(__file__)
REPO_ROOT = os.path.dirname(HERE)  # mod-066-map-art-maker/

SS, OUT = 4, 512
N = OUT * SS  # 2048 supersample canvas

# ---------------------------------------------------------------------------
# Palette — sampled from block-candidates/d1_seamed/top.png and
# front-candidates/f1_drawer/front.png. No new hues.
# ---------------------------------------------------------------------------
WOOD_LIGHT = (152, 116, 70)
WOOD_MED = (140, 106, 64)
WOOD_DARK = (74, 53, 30)

BLUE = (55, 55, 190)
BLUE_MID = (44, 44, 152)
BLUE_DARK = (9, 9, 144)
BLUE_DARKEST = (0, 0, 106)

GREEN = (72, 108, 34)
GREEN_LIGHT = (96, 134, 44)

SAND_LIGHT = (196, 180, 120)

WATER_SET = {BLUE, BLUE_MID, BLUE_DARK, BLUE_DARKEST}
LAND_SET = {GREEN, GREEN_LIGHT, SAND_LIGHT}


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
    """Darken water pixels that 4-touch a land pixel so the shoreline reads
    as a boundary line, the way vanilla map rendering does."""
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
) -> tuple[int, int, int, int]:
    arr = grid_to_array(grid)
    gh, gw = arr.shape[0], arr.shape[1]
    im = Image.fromarray(arr, mode="RGBA").resize(
        (gw * scale, gh * scale), Image.NEAREST
    )
    w, h = im.size
    x0, y0 = int(cx - w / 2), int(cy - h / 2)
    canvas.alpha_composite(im, (x0, y0))
    return x0, y0, x0 + w, y0 + h


def build_easel() -> Image.Image:
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


master_ss = build_easel()
master = master_ss.resize((OUT, OUT), Image.LANCZOS)

for sz in (512, 256, 128, 64):
    master.resize((sz, sz), Image.LANCZOS).save(
        os.path.join(HERE, f"map_art_maker_icon_{sz}.png")
    )
    print("saved", f"map_art_maker_icon_{sz}.png")

injar = master.resize((256, 256), Image.LANCZOS)
injar.save(os.path.join(HERE, "logo_injar_256.png"))
print("saved logo_injar_256.png (staging copy)")

injar_dest = os.path.join(
    REPO_ROOT, "common", "src", "main", "resources", "map_art_maker.png"
)
injar.save(injar_dest)
print("saved", injar_dest, "(wired: common resources reach both loader jars)")
