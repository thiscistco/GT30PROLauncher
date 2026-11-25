#!/usr/bin/env python3
"""Check that recoloring is limited to the original orange areas. Requires Pillow."""
from pathlib import Path
import subprocess
import sys
import tempfile
from PIL import Image, ImageChops, ImageDraw

preview = Path('preview/assets/gt30pro-internals.png')
assert preview.read_bytes() == Path('app/src/main/assets/wallpaper_gt30pro.png').read_bytes()
with tempfile.TemporaryDirectory() as temp:
    original = Path(temp) / 'corrected.png'
    generated = Path(temp) / 'copper.png'
    subprocess.run([sys.executable, 'tools/edit_chip_label.py', 'assets/wallpaper-source/sketch1795646565939.jpeg', str(original)], check=True)
    subprocess.run([sys.executable, 'tools/recolor_copper.py', str(original), str(generated)], check=True)
    before = Image.open(original).convert('RGB')
    after = Image.open(preview).convert('RGB')
    assert ImageChops.difference(after, Image.open(generated).convert('RGB')).getbbox() is None, 'Recoloring is not reproducible'
    difference = ImageChops.difference(before, after).convert('L')
    assert difference.getbbox(), 'No wallpaper changes detected'
    allowed = Image.new('L', after.size, 0)
    draw = ImageDraw.Draw(allowed)
    for x0, y0, x1, y1 in [(0,110,465,235), (840,205,1090,875), (330,785,635,950),
                           (0,180,42,920), (0,920,15,2350), (1209,150,1224,2350), (0,2350,100,2450)]:
        draw.rectangle((x0, y0, x1-1, y1-1), fill=255)
    assert ImageChops.multiply(difference, ImageChops.invert(allowed)).getbbox() is None, 'Changed pixels outside orange regions'
    for name, box in {'battery':(30,1060,1195,2300), 'left flex':(35,925,330,1060),
                      'right flex':(900,875,1120,1080), 'upper flex':(900,40,1080,200),
                      'camera flex':(465,110,650,235), 'chip label':(523,695,698,740)}.items():
        assert difference.crop(box).getbbox() is None, f'Changed protected {name}'
    for x, y in [(1040,270), (1017,750), (470,944)]:
        r0, g0, _ = before.getpixel((x, y))
        r1, g1, _ = after.getpixel((x, y))
        assert r1 - g1 < (r0 - g0) / 2, f'Orange remains at {x},{y}'
print('Copper recoloring is reproducible; protected wallpaper regions unchanged: OK')
