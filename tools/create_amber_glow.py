#!/usr/bin/env python3
"""Generate a transparent animation overlay for the current wallpaper's LEDs.

Usage: python tools/create_amber_glow.py WALLPAPER.png OVERLAY.png
The source wallpaper is not modified. Regions restrict the halo to circuit lights
and frame edges; in particular, the battery and gold/black flex cables are excluded.
Requires Pillow.
"""
import sys
from PIL import Image, ImageChops, ImageDraw, ImageFilter

if len(sys.argv) != 3:
    raise SystemExit(__doc__)
source = Image.open(sys.argv[1]).convert('RGB')
if source.size != (1224, 2720):
    raise SystemExit('Expected the 1224x2720 wallpaper.')
w, h = source.size
regions = [
    (0, 110, 415, 235),     # upper-left illuminated traces
    (840, 205, 1090, 875),  # upper-right motif and circuit trace
    (330, 785, 635, 950),   # lower edge of motherboard
    (0, 180, 42, 920),      # left frame lights, above battery
    (0, 920, 15, 2350),     # slim outer frame; no battery
    (1209, 150, 1224, 2350),# slim right outer frame
    (0, 2350, 100, 2450),   # lower-left frame light
]
allowed = Image.new('L', (w, h), 0)
draw = ImageDraw.Draw(allowed)
for x0, y0, x1, y1 in regions:
    draw.rectangle((x0, y0, x1 - 1, y1 - 1), fill=255)
# Copper strips are not LEDs. Keep them completely free of overlay.
draw.rectangle((375, 130, 415, 235), fill=0)
draw.rectangle((0, 920, 330, 1060), fill=0)
draw.rectangle((900, 875, 1120, 1080), fill=0)
mask = Image.new('L', (w, h), 0)
source_px = source.load(); mask_px = mask.load(); allow_px = allowed.load()
for x0, y0, x1, y1 in regions:
    for y in range(y0, y1):
        for x in range(x0, x1):
            if not allow_px[x, y]:
                continue
            r, g, b = source_px[x, y]
            # Red-orange emitters, not yellow/gold copper or neutral highlights.
            if r > 110 and r - g > 35 and r - b > 52 and g > b * 1.05:
                strength = min(1.0, (r - g - 35) / 50) * min(1.0, (r - 110) / 80)
                mask_px[x, y] = max(mask_px[x, y], int(160 * strength))
soft = mask.filter(ImageFilter.GaussianBlur(7))
# A small sharp core plus soft halo, clipped again after blur to protected areas.
alpha = ImageChops.lighter(mask.point(lambda v: int(v * 0.48)), soft.point(lambda v: min(180, int(v * 1.6))))
alpha = ImageChops.multiply(alpha, allowed)
overlay = Image.new('RGBA', (w, h), (255, 125, 67, 0))
overlay.putalpha(alpha)
overlay.save(sys.argv[2], optimize=True)
print('Overlay:', overlay.size, 'max alpha:', alpha.getextrema()[1], 'bbox:', alpha.getbbox())
