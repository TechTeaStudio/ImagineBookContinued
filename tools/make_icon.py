"""Draw the ImagineBook Continued icon: an open book with a picture on the left page, 32x32 pixel art.

Usage (from the repository root): python tools/make_icon.py
Writes the mod icon (128 px) and the Modrinth project icon (512 px), both scaled without smoothing.
"""
from pathlib import Path
from PIL import Image

N = 32
img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
px = img.load()


def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if 0 <= x < N and 0 <= y < N:
                px[x, y] = c


def dot(x, y, c):
    px[x, y] = c


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


BG, BG_HI, BG_LO = hexc("#243a5e"), hexc("#2f4c78"), hexc("#182a45")
COVER, COVER_LO, COVER_HI = hexc("#7a4421"), hexc("#552d14"), hexc("#94572c")
PAGE, PAGE_SH, PAGE_EDGE = hexc("#f6ead0"), hexc("#dfcda5"), hexc("#c4ad80")
FRAME, FRAME_HI = hexc("#6b4a2a"), hexc("#8f6a3e")
SKY, SKY_HI = hexc("#6fb8ec"), hexc("#9bd2f5")
SUN, SUN_HI = hexc("#ffcf3a"), hexc("#fff08a")
HILL, HILL_LO, HILL_HI = hexc("#4fa23a"), hexc("#357a28"), hexc("#73c24f")
MOUNT, MOUNT_HI = hexc("#8c96a8"), hexc("#e8eef6")
INK = hexc("#9a8662")
FEATHER, FEATHER_SH, QUILL = hexc("#ffffff"), hexc("#c9d2de"), hexc("#3a2a1a")

# Tile: rounded square with a lit top edge and a shaded bottom edge.
rect(1, 0, 30, 31, BG)
rect(0, 1, 31, 30, BG)
rect(2, 0, 29, 0, BG_HI)
rect(0, 2, 0, 29, BG_HI)
rect(2, 31, 29, 31, BG_LO)
rect(31, 2, 31, 29, BG_LO)
dot(1, 1, BG_HI)
dot(30, 30, BG_LO)

# Cover behind the pages.
rect(2, 7, 29, 26, COVER)
rect(2, 26, 29, 26, COVER_LO)
rect(2, 7, 29, 7, COVER_HI)
rect(15, 25, 16, 27, COVER_LO)

# Pages, slightly curved toward the spine.
rect(3, 6, 14, 24, PAGE)
rect(17, 6, 28, 24, PAGE)
rect(3, 25, 14, 25, PAGE_EDGE)
rect(17, 25, 28, 25, PAGE_EDGE)
rect(13, 6, 14, 24, PAGE_SH)
rect(17, 6, 18, 24, PAGE_SH)
rect(15, 6, 16, 25, COVER_LO)
rect(3, 5, 13, 5, PAGE)
rect(18, 5, 28, 5, PAGE)

# Left page: a framed landscape.
rect(4, 8, 13, 19, FRAME)
rect(4, 8, 13, 8, FRAME_HI)
rect(5, 9, 12, 18, SKY)
rect(5, 9, 12, 10, SKY_HI)
rect(10, 10, 11, 11, SUN)
dot(10, 10, SUN_HI)
# mountain
for i, (x0, x1) in enumerate([(7, 7), (6, 8), (5, 9), (5, 10)]):
    rect(x0, 12 + i, x1, 12 + i, MOUNT)
dot(7, 12, MOUNT_HI)
dot(6, 13, MOUNT_HI)
dot(7, 13, MOUNT_HI)
# hills
rect(5, 16, 12, 18, HILL)
rect(8, 15, 12, 15, HILL)
rect(10, 14, 12, 14, HILL_HI)
rect(5, 18, 12, 18, HILL_LO)
dot(9, 15, HILL_HI)
dot(6, 16, HILL_HI)

# Caption lines under the picture.
rect(4, 21, 11, 21, INK)
rect(4, 23, 9, 23, INK)

# Right page: text lines.
for y, x1 in [(8, 27), (10, 25), (12, 27), (14, 24), (16, 27), (18, 22)]:
    rect(19, y, x1, y, INK)

# Quill across the lower right.
for i in range(7):
    x, y = 23 + i, 25 - i
    dot(x, y, FEATHER)
    if i < 6:
        dot(x + 1, y, FEATHER_SH)
    if 1 <= i <= 5:
        dot(x, y - 1, FEATHER)
dot(22, 26, QUILL)
dot(21, 27, QUILL)
dot(30, 18, FEATHER)

ROOT = Path(__file__).resolve().parent.parent
img.resize((128, 128), Image.NEAREST).save(ROOT / "src/main/resources/assets/imaginebook/icon.png")
(ROOT / "docs/modrinth").mkdir(parents=True, exist_ok=True)
img.resize((512, 512), Image.NEAREST).save(ROOT / "docs/modrinth/icon-512.png")
