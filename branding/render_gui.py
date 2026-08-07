"""Map Art Maker GUI background.

Drawn here rather than copied: MineTexture ships extracted vanilla GUI PNGs, and pasting one into
a mod would redistribute Mojang's artwork. What carries over is the convention, measured from the
vanilla containers - panel fill 198, a 1px black outer edge, a 2px white lip top-left and a 2px
dark lip bottom-right, and slot wells that are 18x18 with a dark top-left border and a white
bottom-right bevel around a 16x16 field of 139.

Geometry mirrors MapArtMakerMenu. The panel is 176x224 because the screen stacks a URL field and a
button row above the slots; see the menu for why every output slot has to stay uncovered.
"""

import os

from PIL import Image

OUT = os.path.dirname(os.path.abspath(__file__))

SHEET = 256  # Minecraft blits GUI backgrounds out of a 256x256 sheet
PANEL_W, PANEL_H = 176, 224

BLACK = (0, 0, 0, 255)
PANEL = (198, 198, 198, 255)
LIP_HI = (255, 255, 255, 255)
LIP_LO = (85, 85, 85, 255)
SLOT_EDGE = (55, 55, 55, 255)
SLOT_FILL = (139, 139, 139, 255)

BLANK_SLOT = (26, 92)
OUTPUT_ORIGIN = (98, 74)
SLOT_PITCH = 18
PLAYER_INVENTORY_Y = PANEL_H - 82
HOTBAR_Y = PANEL_H - 24


def panel(img):
    px = img.load()
    for y in range(PANEL_H):
        for x in range(PANEL_W):
            px[x, y] = PANEL
    for i in range(PANEL_W):  # outer edge
        px[i, 0] = BLACK
        px[i, PANEL_H - 1] = BLACK
    for i in range(PANEL_H):
        px[0, i] = BLACK
        px[PANEL_W - 1, i] = BLACK
    for y in range(1, PANEL_H - 1):  # 2px lip, light from the top left
        for x in range(1, PANEL_W - 1):
            if y == 1 and x == 1:
                px[x, y] = BLACK
            elif x <= 2 or y <= 2:
                px[x, y] = LIP_HI
    for y in range(1, PANEL_H - 1):
        for x in range(1, PANEL_W - 1):
            if x >= PANEL_W - 3 or y >= PANEL_H - 3:
                px[x, y] = BLACK if (x == PANEL_W - 2 and y == PANEL_H - 2) else LIP_LO


def slot(img, content_x, content_y):
    """An 18x18 well drawn one pixel out from the 16x16 content box, as vanilla does."""
    px = img.load()
    x0, y0 = content_x - 1, content_y - 1
    for i in range(17):
        px[x0 + i, y0] = SLOT_EDGE
        px[x0, y0 + i] = SLOT_EDGE
    for y in range(1, 17):
        for x in range(1, 17):
            px[x0 + x, y0 + y] = SLOT_FILL
    for i in range(1, 18):  # bottom-right bevel
        px[x0 + 17, y0 + i] = LIP_HI
        px[x0 + i, y0 + 17] = LIP_HI
    px[x0 + 17, y0] = SLOT_FILL
    px[x0, y0 + 17] = SLOT_FILL


def arrow(img, x0, y0):
    """Blank map to finished art, left to right. Flat greys, the way vanilla draws its arrows."""
    px = img.load()
    shaft_top, shaft_bottom = 5, 9
    for x in range(0, 14):
        for y in range(shaft_top, shaft_bottom + 1):
            px[x0 + x, y0 + y] = SLOT_FILL
        px[x0 + x, y0 + shaft_top - 1] = SLOT_EDGE
        px[x0 + x, y0 + shaft_bottom + 1] = SLOT_EDGE
    for i in range(8):  # head
        top, bottom = i, 14 - i
        for y in range(top, bottom + 1):
            px[x0 + 14 + i, y0 + y] = SLOT_FILL
        px[x0 + 14 + i, y0 + top] = SLOT_EDGE
        px[x0 + 14 + i, y0 + bottom] = SLOT_EDGE
    for y in range(0, 15):  # back edge of the head, above and below the shaft
        if y < shaft_top - 1 or y > shaft_bottom + 1:
            px[x0 + 14, y0 + y] = SLOT_EDGE


def build():
    img = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
    panel(img)
    slot(img, *BLANK_SLOT)
    arrow(img, 55, 92)
    for row in range(3):
        for column in range(3):
            slot(
                img,
                OUTPUT_ORIGIN[0] + column * SLOT_PITCH,
                OUTPUT_ORIGIN[1] + row * SLOT_PITCH,
            )
    for row in range(3):
        for column in range(9):
            slot(img, 8 + column * SLOT_PITCH, PLAYER_INVENTORY_Y + row * SLOT_PITCH)
    for column in range(9):
        slot(img, 8 + column * SLOT_PITCH, HOTBAR_Y)
    return img


if __name__ == "__main__":
    import sys

    target = (
        sys.argv[1]
        if len(sys.argv) > 1
        else os.path.join(
            os.path.dirname(OUT),
            "common",
            "src",
            "main",
            "resources",
            "assets",
            "map_art_maker",
            "textures",
            "gui",
        )
    )
    os.makedirs(target, exist_ok=True)
    path = os.path.join(target, "map_art_maker.png")
    build().save(path)
    print("wrote", path)
