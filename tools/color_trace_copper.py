#!/usr/bin/env python3
"""Recolor only the user-drawn animation paths to subdued metallic copper.

Usage: python tools/color_trace_copper.py WALLPAPER.png PATHS.json OUTPUT.png
Apply to the previously corrected/recolored wallpaper, not to this tool's output.
Path widths and coordinates come from the supplied animation configuration;
original image shading is retained, with battery and flex cables protected.
Requires Pillow.
"""
import json
import sys
from PIL import Image, ImageChops, ImageDraw

if len(sys.argv) != 4:
    raise SystemExit(__doc__)
image = Image.open(sys.argv[1]).convert('RGB')
config = json.load(open(sys.argv[2], encoding='utf-8'))
if image.size != (1224, 2720) or (config['width'], config['height']) != image.size:
    raise SystemExit('Expected the 1224x2720 wallpaper and matching path coordinates.')
# Antialias the painted path edge without affecting unrelated wallpaper pixels.
scale = 2
mask = Image.new('L', (image.width * scale, image.height * scale), 0)
draw = ImageDraw.Draw(mask)
seen = set()
for path in config['paths']:
    if path.get('kind') != 'copper':
        continue
    width = path.get('width', 10)
    if not isinstance(width, int) or isinstance(width, bool) or not 4 <= width <= 60:
        raise SystemExit('Path width must be an integer between 4 and 60.')
    points = tuple(tuple(p) for p in path['points'])
    if len(points) < 2 or any(len(p) != 2 or any(not isinstance(v, int) or isinstance(v, bool) for v in p)
                              or not (0 <= p[0] < image.width and 0 <= p[1] < image.height) for p in points):
        raise SystemExit('Every path must contain at least two in-bounds integer points.')
    if (points, width) in seen:
        continue
    seen.add((points, width))
    scaled = [(x * scale, y * scale) for x, y in points]
    draw.line(scaled, fill=255, width=width * scale, joint='curve')
    radius = width * scale / 2
    for x, y in scaled:
        draw.ellipse((x-radius, y-radius, x+radius, y+radius), fill=255)
mask = mask.resize(image.size, Image.Resampling.LANCZOS)
# Enforce the same battery/flex exclusions even if a future path crosses them.
protected = Image.new('L', image.size, 255)
guard = ImageDraw.Draw(protected)
for box in [(30,1060,1195,2300), (35,925,330,1060), (900,875,1120,1080),
            (900,40,1080,200), (465,110,650,235)]:
    guard.rectangle(box, fill=0)
mask = ImageChops.multiply(mask, protected)
# The added copper layer is only 10% opaque; original wallpaper details and
# colors remain visible beneath it. This does not affect the animation overlay.
mask = mask.point(lambda value: round(value * 0.10))
# The existing metallic trace is a muted beige-copper. Preserve local shading,
# compressing its luminance range so darker traversed areas still read as copper.
source = image.load()
colors = Image.new('RGB', image.size)
target = colors.load()
bounds = mask.getbbox()
mask_pixels = mask.load()
if bounds:
    x0, y0, x1, y1 = bounds
    for y in range(y0, y1):
        for x in range(x0, x1):
            if not mask_pixels[x, y]:
                continue
            r, g, b = source[x, y]
            # Slightly darker than the previous 145 baseline to blend with
            # the existing copper traces while keeping their local shading.
            level = 132 + 0.18 * (0.299*r + 0.587*g + 0.114*b)
            target[x, y] = (round(level * 1.08), round(level * 1.01), round(level * 0.85))
image.paste(colors, (0, 0), mask)
image.save(sys.argv[3], format='PNG', optimize=True)
