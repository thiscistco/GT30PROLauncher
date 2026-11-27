#!/usr/bin/env python3
"""Generate identical black/white glass overlays for Android and preview.

75% transparency means 25% opacity (64/255, rounded to the nearest byte).
The artwork is separate from the wallpaper, without a fake camera hole.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
W, H = 1224, 2720

def artwork(dark=False):
    color = 0 if dark else 255
    return Image.new('RGBA', (W, H), (color, color, color, 64))

if __name__ == '__main__':
    for dark, android_suffix, preview_suffix in [(False, '', ''), (True, '_dark', '-dark')]:
        image = artwork(dark)
        image.save(ROOT / f'app/src/main/assets/home_screen_glass{android_suffix}.png', optimize=True)
        image.save(ROOT / f'preview/assets/home-screen-glass{preview_suffix}.png', optimize=True)
