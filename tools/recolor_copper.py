#!/usr/bin/env python3
"""Recolor the wallpaper's orange emitters to muted metallic copper.

Usage: python tools/recolor_copper.py CORRECTED_WALLPAPER.png OUTPUT.png
Start with the label-corrected original (tools/edit_chip_label.py), not the
output of a previous recoloring. Changes are limited to orange pixels in the
motherboard/frame regions; the battery and flex cables are protected. The
animation overlay is generated separately from the original corrected source.
Requires Pillow.
"""
import sys
from PIL import Image, ImageDraw

if len(sys.argv) != 3:
    raise SystemExit(__doc__)
image = Image.open(sys.argv[1]).convert('RGB')
if image.size != (1224, 2720):
    raise SystemExit('Expected the 1224x2720 corrected wallpaper.')
regions = [
    (0, 110, 465, 235), (840, 205, 1090, 875), (330, 785, 635, 950),
    (0, 180, 42, 920), (0, 920, 15, 2350), (1209, 150, 1224, 2350),
    (0, 2350, 100, 2450),
]
allowed = Image.new('L', image.size, 0)
draw = ImageDraw.Draw(allowed)
for x0, y0, x1, y1 in regions:
    draw.rectangle((x0, y0, x1 - 1, y1 - 1), fill=255)
for box in [(30, 1060, 1195, 2300), (35, 925, 330, 1060),
            (900, 875, 1120, 1080), (900, 40, 1080, 200), (465, 110, 650, 235)]:
    draw.rectangle(box, fill=0)
pixels = image.load()
allowed_pixels = allowed.load()
for x0, y0, x1, y1 in regions:
    # Regions can overlap; consume each pixel at most once.
    for y in range(y0, y1):
        for x in range(x0, x1):
            if not allowed_pixels[x, y]:
                continue
            allowed_pixels[x, y] = 0
            r, g, b = pixels[x, y]
            # Soften the transition for low-saturation, shadow and halo pixels.
            strength = max(0, min(1, (r - g - 12) / 30)) * max(0, min(1, (r - b - 18) / 35))
            if not strength:
                continue
            luminance = 0.299 * r + 0.587 * g + 0.114 * b
            # Sampled metallic PCB trace is warm, subdued beige-copper, not
            # orange-emissive. Preserve source shading and detail per pixel.
            copper = (luminance * 1.10, luminance * 1.03, luminance * 0.86)
            pixels[x, y] = tuple(max(0, min(255, round(old * (1-strength) + new * strength)))
                                 for old, new in zip((r, g, b), copper))
image.save(sys.argv[2], format='PNG', optimize=True)
