#!/usr/bin/env python3
"""Check orange routes, nearby connected traces, and protected wallpaper regions."""
from pathlib import Path
import json
import subprocess
import sys
import tempfile
from PIL import Image, ImageChops, ImageDraw, ImageFilter

preview = Path('preview/assets/gt30pro-internals.png')
assert preview.read_bytes() == Path('app/src/main/assets/wallpaper_gt30pro.png').read_bytes()
with tempfile.TemporaryDirectory() as temp:
    original = Path(temp) / 'corrected.png'
    expected = Path(temp) / 'traces.png'
    subprocess.run([sys.executable, 'tools/edit_chip_label.py', 'assets/wallpaper-source/sketch1795646565939.jpeg', str(original)], check=True)
    subprocess.run([sys.executable, 'tools/color_trace_orange.py', str(original), 'preview/assets/circuit-paths.json', str(expected)], check=True)
    before = Image.open(original).convert('RGB')
    after = Image.open(preview).convert('RGB')
    assert ImageChops.difference(after, Image.open(expected).convert('RGB')).getbbox() is None, 'Orange wallpaper must be reproducible'
    difference = ImageChops.difference(before, after).convert('L')
    assert difference.getbbox(), 'No recoloring detected'
    for name, box in {'battery':(30,1060,1195,2300), 'left flex':(35,925,330,1060),
                      'right flex':(900,875,1120,1080), 'upper flex':(900,40,1080,200),
                      'camera flex':(465,110,650,235), 'chip label':(523,695,698,740)}.items():
        assert difference.crop(box).getbbox() is None, f'Recoloring changed protected {name}'
    band = Image.new('L', after.size, 0)
    narrow = Image.new('L', after.size, 0)
    draw, core = ImageDraw.Draw(band), ImageDraw.Draw(narrow)
    paths = json.loads(Path('preview/assets/circuit-paths.json').read_text())['paths']
    for path in paths[::3]:
        points = [tuple(point) for point in path['points']]
        width = path['width'] + 24
        draw.line(points, fill=255, width=width, joint='curve')
        core.line(points, fill=255, width=path['width'] + 2, joint='curve')
        for x,y in points:
            draw.ellipse((x-width/2,y-width/2,x+width/2,y+width/2), fill=255)
            radius = (path['width'] + 2) / 2
            core.ellipse((x-radius,y-radius,x+radius,y+radius), fill=255)
        if not any(difference.getpixel(p) > 0 for p in points):
            assert all(before.getpixel(p)[0] - before.getpixel(p)[1] >= 50 and
                       before.getpixel(p)[0] - before.getpixel(p)[2] >= 75 for p in points), 'Route was neither orange nor recolored'
    allowed = band.filter(ImageFilter.GaussianBlur(3)).point(lambda v: 255 if v else 0)
    assert ImageChops.multiply(difference, ImageChops.invert(allowed)).getbbox() is None, 'Changed pixels away from routes and nearby connected traces'
    assert ImageChops.multiply(difference, ImageChops.invert(narrow)).getbbox() is not None, 'Connected warm areas around the route should also be recolored'
    # Already-orange artwork supplies the reference and must remain unchanged.
    for point in [(1040,270), (1017,750), (470,944)]:
        r,g,b = before.getpixel(point)
        assert r-g >= 50 and r-b >= 75
        assert after.getpixel(point) == (r,g,b), f'Original orange reference changed at {point}'
print('Orange routes and nearby warm traces preserve source orange, labels, battery and cables: OK')
