#!/usr/bin/env python3
"""Generate a transparent animation overlay for the current wallpaper's LEDs.

Usage: python tools/create_amber_glow.py WALLPAPER.png OVERLAY.png [PATHS.json]
The source wallpaper is not modified. Circuit paths default to the shared
preview configuration; an exported preview configuration can be supplied.
Battery and gold/black flex cables are excluded.
Requires Pillow.
"""
import json
import sys
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter

if len(sys.argv) not in (3, 4):
    raise SystemExit(__doc__)
source = Image.open(sys.argv[1]).convert('RGB')
if source.size != (1224, 2720):
    raise SystemExit('Expected the 1224x2720 wallpaper.')
w, h = source.size
config_path = Path(sys.argv[3]) if len(sys.argv) == 4 else Path(__file__).resolve().parents[1] / 'preview/assets/circuit-paths.json'
config = json.loads(config_path.read_text())
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
# Legacy orange emitters are used only when explicitly requested by a path.
for x0, y0, x1, y1 in (regions if any(p.get('kind') == 'amber' for p in config['paths']) else []):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if not allow_px[x, y]:
                continue
            r, g, b = source_px[x, y]
            # Red-orange emitters, not yellow/gold copper or neutral highlights.
            if r > 110 and r - g > 35 and r - b > 52 and g > b * 1.05:
                strength = min(1.0, (r - g - 35) / 50) * min(1.0, (r - 110) / 80)
                mask_px[x, y] = max(mask_px[x, y], int(200 * strength))
soft = mask.filter(ImageFilter.GaussianBlur(8))
# Brighter neon orange, with a sharp core and restrained halo.
alpha = ImageChops.lighter(mask.point(lambda v: int(v * 0.70)), soft.point(lambda v: min(220, int(v * 2.0))))
alpha = ImageChops.multiply(alpha, allowed)
overlay = Image.new('RGBA', (w, h), (255, 115, 36, 0))
overlay.putalpha(alpha)

# Paint copper highlights only for the paths supplied in the configuration.
copper_core = Image.new('L', (w, h), 0)
copper_draw = ImageDraw.Draw(copper_core)
seen = set()
for path in config['paths']:
    if path.get('kind') != 'copper':
        continue
    points = tuple(tuple(point) for point in path['points'])
    width = path.get('width', 10)
    if not isinstance(width, int) or isinstance(width, bool) or not 4 <= width <= 60:
        raise SystemExit('Copper path width must be an integer from 4 to 60 pixels.')
    if (points, width) in seen:
        continue
    seen.add((points, width))
    copper_draw.line(points, fill=255, width=width, joint='curve')
    radius = width / 2
    for x, y in points:
        copper_draw.ellipse((x-radius, y-radius, x+radius, y+radius), fill=255)
protected = Image.new('L', (w, h), 255)
guard = ImageDraw.Draw(protected)
for box in [(30, 1060, 1195, 2300), (35, 925, 330, 1060), (900, 875, 1120, 1080), (900, 40, 1080, 200)]:
    guard.rectangle(box, fill=0)
# Orange neon halo and a luminous golden-amber centre, both kept inside safe areas.
halo_alpha = copper_core.filter(ImageFilter.GaussianBlur(9)).point(lambda v: min(210, int(v * 2.2)))
halo_alpha = ImageChops.multiply(halo_alpha, protected)
halo = Image.new('RGBA', (w, h), (255, 105, 25, 0))
halo.putalpha(halo_alpha)
overlay = Image.alpha_composite(overlay, halo)
core_alpha = copper_core.filter(ImageFilter.GaussianBlur(0.8)).point(lambda v: int(v * 0.92))
core_alpha = ImageChops.multiply(core_alpha, protected)
copper = Image.new('RGBA', (w, h), (255, 205, 106, 0))
copper.putalpha(core_alpha)
overlay = Image.alpha_composite(overlay, copper)
overlay.save(sys.argv[2], optimize=True)
print('Overlay:', overlay.size, 'max alpha:', overlay.getchannel('A').getextrema()[1], 'bbox:', overlay.getchannel('A').getbbox())
