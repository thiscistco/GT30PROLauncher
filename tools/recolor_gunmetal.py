#!/usr/bin/env python3
"""Recolor original orange circuit emitters and their halos to gunmetal.

Usage: python tools/recolor_gunmetal.py CORRECTED_WALLPAPER.png PATHS.json OUTPUT.png
Start with the label-corrected original (tools/edit_chip_label.py), not the
output of a previous recoloring. Changes are limited to orange pixels in the
motherboard/frame regions; the battery and flex cables are protected. The
animation overlay is generated separately from the original corrected source.
Requires Pillow.
"""
import json
import sys
from PIL import Image, ImageChops, ImageDraw, ImageFilter

if len(sys.argv) != 4:
    raise SystemExit(__doc__)
image = Image.open(sys.argv[1]).convert('RGB')
config = json.load(open(sys.argv[2], encoding='utf-8'))
if image.size != (1224, 2720) or (config['width'], config['height']) != image.size:
    raise SystemExit('Expected a 1224x2720 wallpaper and matching path coordinates.')
regions = [
    (0, 110, 465, 235), (840, 205, 1090, 875), (330, 785, 700, 995),
    (0, 180, 42, 920), (0, 920, 15, 2350), (1209, 150, 1224, 2350),
    (0, 2350, 100, 2450),
]
allowed = Image.new('L', image.size, 0)
draw = ImageDraw.Draw(allowed)
for x0, y0, x1, y1 in regions:
    draw.rectangle((x0, y0, x1 - 1, y1 - 1), fill=255)
# Feather the edge of the allowed circuit regions, then apply hard protected
# exclusions. This prevents visible rectangular color boundaries.
allowed = allowed.filter(ImageFilter.GaussianBlur(16))
protected = Image.new('L', image.size, 255)
guard = ImageDraw.Draw(protected)
for box in [(30, 1060, 1195, 2300), (35, 925, 330, 1060),
            (900, 875, 1120, 1080), (900, 40, 1080, 200), (465, 110, 650, 235)]:
    guard.rectangle(box, fill=0)
allowed = ImageChops.multiply(allowed, protected)
# Neutralize warm copper immediately beside the drawn animation route too;
# this avoids a leftover gold stripe without painting a broad flat line.
near = Image.new('L', image.size, 0)
near_draw = ImageDraw.Draw(near)
seen = set()
for path in config['paths']:
    if path.get('kind') != 'copper':
        continue
    points = tuple(tuple(p) for p in path['points'])
    width = path.get('width', 10)
    if (points, width) in seen:
        continue
    seen.add((points, width))
    band = width + 24
    near_draw.line(points, fill=255, width=band, joint='curve')
    for x, y in points:
        near_draw.ellipse((x-band/2, y-band/2, x+band/2, y+band/2), fill=255)
near = ImageChops.multiply(near.filter(ImageFilter.GaussianBlur(2)), protected)
pixels = image.load()
allowed_pixels = allowed.load()
near_pixels = near.load()
x0, y0, x1, y1 = ImageChops.lighter(allowed, near).getbbox()
for y in range(y0, y1):
    for x in range(x0, x1):
        allowance = allowed_pixels[x, y] / 255.0
        vicinity = near_pixels[x, y] / 255.0
        if not allowance and not vicinity:
            continue
        r, g, b = pixels[x, y]
        # Orange emitters and warm copper each receive a soft chroma-dependent
        # transition, retaining the original luminance and surface texture.
        orange = allowance * max(0, min(1, (r - g - 4) / 18)) * max(0, min(1, (r - b - 8) / 24))
        warm = vicinity * max(0, min(1, (r - g + 4) / 10)) * max(0, min(1, (r - b - 5) / 25))
        strength = max(orange, warm)
        if not strength:
            continue
        luminance = 0.299 * r + 0.587 * g + 0.114 * b
        # Cool, dark gunmetal with the original shading preserved.
        gunmetal = (luminance * 0.72, luminance * 0.82, luminance * 0.88)
        pixels[x, y] = tuple(max(0, min(255, round(old * (1-strength) + new * strength)))
                             for old, new in zip((r, g, b), gunmetal))
image.save(sys.argv[3], format='PNG', optimize=True)
