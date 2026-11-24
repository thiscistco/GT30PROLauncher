#!/usr/bin/env python3
"""Generator ikon launcher (PNG) memakai Pillow.

Dijalankan ulang saat desain ikon berubah:
    python3 tools/gen_icons.py
"""
import math
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

ACCENT = (247, 162, 59, 255)
ACCENT2 = (255, 107, 53, 255)
WHITE = (255, 255, 255, 255)
DARK = (20, 23, 28, 255)
DARK2 = (32, 37, 45, 255)

ICON_TILES = {
    "ic_phone": (76, 175, 80, 255),
    "ic_message": (74, 144, 226, 255),
    "ic_camera": (231, 76, 60, 255),
    "ic_browser": (22, 160, 133, 255),
    "ic_super": ACCENT,
    "ic_apps": ACCENT,
    "ic_settings": ACCENT,
}


def rounded_tile(size, c1, c2=None, radius_ratio=0.24):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    r = int(size * radius_ratio)
    if c2 is None:
        ImageDraw.Draw(img).rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=c1)
    else:
        grad = Image.new("RGBA", (size, size))
        gd = ImageDraw.Draw(grad)
        for y in range(size):
            t = y / (size - 1)
            col = tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(4))
            gd.line([(0, y), (size, y)], fill=col)
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=255)
        img.paste(grad, (0, 0), mask)
    return img


def glyph_phone(d, s, col):
    w = max(3, s // 26)
    bw, bh = int(s * 0.36), int(s * 0.60)
    x0, y0 = (s - bw) // 2, (s - bh) // 2
    d.rounded_rectangle([x0, y0, x0 + bw, y0 + bh], radius=int(s * 0.08), outline=col, width=w)
    d.line([(x0 + bw // 3, y0 + int(s * 0.07)), (x0 + 2 * bw // 3, y0 + int(s * 0.07))], fill=col, width=w)
    cy = y0 + bh - int(s * 0.08)
    d.ellipse([x0 + bw // 2 - w, cy - w, x0 + bw // 2 + w, cy + w], fill=col)


def glyph_message(d, s, col):
    w = max(3, s // 26)
    x0, y0, x1, y1 = int(s * 0.20), int(s * 0.24), int(s * 0.80), int(s * 0.62)
    d.rounded_rectangle([x0, y0, x1, y1], radius=int(s * 0.10), outline=col, width=w)
    d.polygon([(int(s * 0.32), y1 - 2), (int(s * 0.32), int(s * 0.80)), (int(s * 0.54), y1 - 2)], fill=col)
    for i in range(3):
        cx = int(s * 0.34) + i * int(s * 0.12)
        cy = (y0 + y1) // 2
        d.ellipse([cx - w, cy - w, cx + w, cy + w], fill=col)


def glyph_camera(d, s, col):
    w = max(3, s // 26)
    x0, y0, x1, y1 = int(s * 0.18), int(s * 0.30), int(s * 0.82), int(s * 0.72)
    d.rounded_rectangle([x0, y0, x1, y1], radius=int(s * 0.08), outline=col, width=w)
    d.rounded_rectangle([int(s * 0.36), y0 - int(s * 0.07), int(s * 0.64), y0 + 2], radius=int(s * 0.03), outline=col, width=w)
    r = int(s * 0.11)
    cx, cy = s // 2, (y0 + y1) // 2
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=col, width=w)


def glyph_browser(d, s, col):
    w = max(3, s // 26)
    r = int(s * 0.30)
    cx = cy = s // 2
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=col, width=w)
    d.ellipse([cx - r // 2, cy - r, cx + r // 2, cy + r], outline=col, width=max(2, w - 1))
    d.line([(cx - r, cy), (cx + r, cy)], fill=col, width=max(2, w - 1))


def glyph_apps(d, s, col):
    g = int(s * 0.12)
    gap = int(s * 0.07)
    x0 = y0 = (s - (3 * g + 2 * gap)) // 2
    for row in range(3):
        for c in range(3):
            x = x0 + c * (g + gap)
            y = y0 + row * (g + gap)
            d.rounded_rectangle([x, y, x + g, y + g], radius=max(2, g // 4), fill=col)


def glyph_super(d, s, col):
    pts = [(0.58, 0.10), (0.28, 0.56), (0.48, 0.56), (0.40, 0.92), (0.74, 0.44), (0.53, 0.44)]
    d.polygon([(int(x * s), int(y * s)) for x, y in pts], fill=col)


def glyph_settings(d, s, col):
    w = max(3, s // 26)
    cx = cy = s // 2
    r = int(s * 0.17)
    spoke = int(s * 0.07)
    for i in range(8):
        ang = math.radians(i * 45)
        x0 = cx + int((r - 2) * math.cos(ang))
        y0 = cy + int((r - 2) * math.sin(ang))
        x1 = cx + int((r + spoke) * math.cos(ang))
        y1 = cy + int((r + spoke) * math.sin(ang))
        d.line([(x0, y0), (x1, y1)], fill=col, width=spoke)
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=col, width=w)
    hole = r // 3
    d.ellipse([cx - hole, cy - hole, cx + hole, cy + hole], fill=col)


GLYPHS = {
    "ic_phone": glyph_phone,
    "ic_message": glyph_message,
    "ic_camera": glyph_camera,
    "ic_browser": glyph_browser,
    "ic_apps": glyph_apps,
    "ic_super": glyph_super,
    "ic_settings": glyph_settings,
}


def make_app_icon(size=160):
    img = rounded_tile(size, DARK, DARK2)
    d = ImageDraw.Draw(img)
    # petir besar sebagai simbol GT30
    pts = [(0.58, 0.14), (0.28, 0.58), (0.48, 0.58), (0.40, 0.92), (0.74, 0.46), (0.53, 0.46)]
    d.polygon([(int(x * size), int(y * size)) for x, y in pts], fill=ACCENT)
    # aksen sudut
    d.rounded_rectangle([size - int(size * 0.30), int(size * 0.10), size - int(size * 0.10), int(size * 0.14)],
                        radius=int(size * 0.02), fill=ACCENT2)
    return img


def make_round_icon(size=160):
    img = make_app_icon(size)
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def save(img, rel_path):
    path = os.path.join(RES, rel_path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "PNG")
    print("  ->", os.path.relpath(path, ROOT))


def main():
    print("Membuat ikon launcher...")
    app_icon = make_app_icon(192)
    round_icon = make_round_icon(192)
    save(app_icon, "mipmap-xxxhdpi/ic_launcher.png")
    save(round_icon, "mipmap-xxxhdpi/ic_launcher_round.png")
    save(app_icon.resize((144, 144), Image.LANCZOS), "mipmap-xxhdpi/ic_launcher.png")
    save(round_icon.resize((144, 144), Image.LANCZOS), "mipmap-xxhdpi/ic_launcher_round.png")

    print("Membuat ikon aplikasi...")
    size = 160
    for name, draw_glyph in GLYPHS.items():
        tile_color = ICON_TILES[name]
        img = rounded_tile(size, tile_color, None, 0.26)
        draw_glyph(ImageDraw.Draw(img), size, WHITE)
        save(img, "drawable-xxhdpi/%s.png" % name)
    print("Selesai.")


if __name__ == "__main__":
    main()
