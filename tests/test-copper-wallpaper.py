#!/usr/bin/env python3
"""Check copper recoloring is limited to the current smoothed animation paths."""
from pathlib import Path
import json
import subprocess
import sys
import tempfile
from PIL import Image, ImageChops, ImageDraw

preview = Path('preview/assets/gt30pro-internals.png')
assert preview.read_bytes() == Path('app/src/main/assets/wallpaper_gt30pro.png').read_bytes()
with tempfile.TemporaryDirectory() as temp:
    original = Path(temp) / 'corrected.png'
    expected = Path(temp) / 'traces.png'
    subprocess.run([sys.executable, 'tools/edit_chip_label.py', 'assets/wallpaper-source/sketch1795646565939.jpeg', str(original)], check=True)
    subprocess.run([sys.executable, 'tools/color_trace_copper.py', str(original), 'preview/assets/circuit-paths.json', str(expected)], check=True)
    before = Image.open(original).convert('RGB')
    after = Image.open(preview).convert('RGB')
    assert ImageChops.difference(after, Image.open(expected).convert('RGB')).getbbox() is None, 'Trace recoloring must be reproducible'
    difference = ImageChops.difference(before, after).convert('L')
    assert difference.getbbox(), 'No trace recoloring detected'
    for name, box in {'battery':(30,1060,1195,2300), 'left flex':(35,925,330,1060),
                      'right flex':(900,875,1120,1080), 'upper flex':(900,40,1080,200),
                      'camera flex':(465,110,650,235), 'chip label':(523,695,698,740)}.items():
        assert difference.crop(box).getbbox() is None, f'Trace recoloring changed protected {name}'
    paths = json.loads(Path('preview/assets/circuit-paths.json').read_text())['paths']
    allowed = Image.new('L', after.size, 0)
    draw = ImageDraw.Draw(allowed)
    for path in paths[::3]:
        points = [tuple(point) for point in path['points']]
        radius = path['width'] / 2 + 4
        draw.line(points, fill=255, width=path['width'] + 8, joint='curve')
        for x, y in points:
            draw.ellipse((x-radius,y-radius,x+radius,y+radius), fill=255)
        assert any(difference.getpixel(p) > 0 for p in points), 'A route was not recolored'
    assert ImageChops.multiply(difference, ImageChops.invert(allowed)).getbbox() is None, 'Changed pixels away from animation paths'
    # Unlike the older global copper conversion, the original orange areas
    # outside the user's paths must remain byte-for-byte unchanged.
    for x, y in [(1040,270), (1017,750), (470,944)]:
        if allowed.getpixel((x,y)) == 0:
            assert after.getpixel((x,y)) == before.getpixel((x,y))
print('Copper matches the smoothed traces; outside pixels, labels, battery and cables preserved: OK')
