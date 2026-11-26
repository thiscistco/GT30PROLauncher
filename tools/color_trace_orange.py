#!/usr/bin/env python3
"""Tint animation routes and adjacent warm circuit traces orange like the source.

Usage: python tools/color_trace_orange.py CORRECTED_WALLPAPER.png PATHS.json OUTPUT.png
Existing orange in the original stays intact; other route pixels and nearby warm
metal get a luminous orange hue with their local shading preserved. Battery,
flex cables and the camera flex are protected. Requires Pillow.
"""
import json
import sys
from PIL import Image, ImageChops, ImageDraw, ImageFilter

if len(sys.argv) != 4:
    raise SystemExit(__doc__)
image = Image.open(sys.argv[1]).convert('RGB')
config = json.load(open(sys.argv[2], encoding='utf-8'))
if image.size != (1224, 2720) or (config['width'], config['height']) != image.size:
    raise SystemExit('Expected a 1224x2720 wallpaper and matching paths.')
scale = 3
core = Image.new('L', (image.width*scale, image.height*scale), 0)
band = Image.new('L', image.size, 0)
core_draw, band_draw = ImageDraw.Draw(core), ImageDraw.Draw(band)
seen = set()
for path in config['paths']:
    if path.get('kind') != 'copper':
        continue
    width = path.get('width', 10)
    points = tuple(tuple(p) for p in path['points'])
    if not isinstance(width, int) or isinstance(width, bool) or not 4 <= width <= 60:
        raise SystemExit('Path width must be an integer between 4 and 60.')
    if len(points) < 2 or any(len(p) != 2 or any(not isinstance(v, int) or isinstance(v, bool) for v in p)
                              or not (0 <= p[0] < image.width and 0 <= p[1] < image.height) for p in points):
        raise SystemExit('Every path must contain at least two in-bounds integer points.')
    if (points, width) in seen:
        continue
    seen.add((points, width))
    scaled = [(x*scale, y*scale) for x,y in points]
    core_draw.line(scaled, fill=255, width=width*scale, joint='curve')
    for x,y in scaled:
        r = width*scale/2
        core_draw.ellipse((x-r,y-r,x+r,y+r), fill=255)
    surrounding = width + 24
    band_draw.line(points, fill=255, width=surrounding, joint='curve')
    for x,y in points:
        r = surrounding/2
        band_draw.ellipse((x-r,y-r,x+r,y+r), fill=255)
core = core.resize(image.size, Image.Resampling.LANCZOS)
band = band.filter(ImageFilter.GaussianBlur(2))
protected = Image.new('L', image.size, 255)
guard = ImageDraw.Draw(protected)
for box in [(30,1060,1195,2300), (35,925,330,1060), (900,875,1120,1080),
            (900,40,1080,200), (465,110,650,235)]:
    guard.rectangle(box, fill=0)
core, band = ImageChops.multiply(core, protected), ImageChops.multiply(band, protected)
pixels = image.load()
core_pixels, band_pixels = core.load(), band.load()
bounds = ImageChops.lighter(core, band).getbbox()
if bounds:
    x0,y0,x1,y1 = bounds
    for y in range(y0,y1):
        for x in range(x0,x1):
            center, nearby = core_pixels[x,y]/255.0, band_pixels[x,y]/255.0
            if not center and not nearby:
                continue
            r,g,b = pixels[x,y]
            # Adjacent warm metal is treated as connected artwork; unrelated
            # gray/blue material beside a route is not repainted.
            warm = max(0, min(1, (r-g+4)/10)) * max(0, min(1, (r-b-5)/25))
            strength = max(center, nearby*warm)
            # Already-orange pixels are the reference and remain unchanged.
            original_orange = max(0, min(1, (r-g-25)/25)) * max(0, min(1, (r-b-40)/35))
            strength *= 1 - original_orange
            if not strength:
                continue
            luminance = .299*r + .587*g + .114*b
            orange = tuple(min(255, luminance * factor) for factor in (1.40, .90, .65))
            pixels[x,y] = tuple(max(0,min(255,round(old*(1-strength) + new*strength)))
                                for old,new in zip((r,g,b), orange))
image.save(sys.argv[3], format='PNG', optimize=True)
