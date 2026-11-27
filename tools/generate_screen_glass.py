#!/usr/bin/env python3
"""Create the same translucent protective screen layer for Android and preview.

The artwork is a neutral glass finish, not an opaque phone image. It is kept
separate from the wallpaper and deliberately does not draw a fake camera hole.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
W, H = 1224, 2720

def coat(image, color, opacity):
    layer = Image.new('RGBA', (W, H), color + (0,))
    layer.putalpha(opacity)
    return Image.alpha_composite(image, layer)

def artwork():
    # A very light cool tint: nearly all the internals remain visible.
    image = Image.new('RGBA', (W, H), (28, 45, 60, 8))
    reflection = Image.new('L', (W, H), 0)
    draw = ImageDraw.Draw(reflection)
    draw.polygon([(-W*.22, H*.04), (W*.04, -H*.04), (W*1.17, H*.48), (W*.94, H*.56)], fill=28)
    reflection = reflection.filter(ImageFilter.GaussianBlur(76))
    image = coat(image, (220, 237, 250), reflection)
    # A second, dimmer reflection adds depth without concealing the components.
    reflection = Image.new('L', (W, H), 0)
    draw = ImageDraw.Draw(reflection)
    draw.polygon([(-W*.15,H*.64), (-W*.03,H*.60), (W*.99,H*.95), (W*.87,H*.99)], fill=12)
    reflection = reflection.filter(ImageFilter.GaussianBlur(55))
    image = coat(image, (200, 225, 244), reflection)
    # A soft dark bevel, with a fine glass rim just inside the actual screen.
    edge = Image.new('RGBA', (W, H), (0,0,0,0)); d = ImageDraw.Draw(edge)
    for i in range(20, 0, -1):
        opacity = max(2, round(4 + (20-i)*2.1))
        d.rounded_rectangle((i,i,W-1-i,H-1-i), radius=105-i,
                            outline=(4,10,16,opacity), width=2)
    image = Image.alpha_composite(image, edge)
    rim = Image.new('RGBA', (W, H), (0,0,0,0)); d = ImageDraw.Draw(rim)
    d.rounded_rectangle((23,23,W-24,H-24), radius=82, outline=(185,211,228,65), width=3)
    d.rounded_rectangle((28,28,W-29,H-29), radius=78, outline=(221,239,250,24), width=2)
    image = Image.alpha_composite(image, rim)
    return image

if __name__ == '__main__':
    image = artwork()
    for name in ['app/src/main/assets/home_screen_glass.png','preview/assets/home-screen-glass.png']:
        image.save(ROOT / name, optimize=True)
