#!/usr/bin/env python3
"""The restored wallpaper is the label-corrected original, with protected areas unchanged."""
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
    copper = Path(temp) / 'copper.png'
    previous = Path(temp) / 'previous.png'
    subprocess.run([sys.executable, 'tools/edit_chip_label.py', 'assets/wallpaper-source/sketch1795646565939.jpeg', str(original)], check=True)
    subprocess.run([sys.executable, 'tools/recolor_copper.py', str(original), str(copper)], check=True)
    subprocess.run([sys.executable, 'tools/color_trace_copper.py', str(copper), 'assets/circuit-paths/Jalur-2.json', str(previous)], check=True)
    restored = Image.open(preview).convert('RGB')
    corrected = Image.open(original).convert('RGB')
    old = Image.open(previous).convert('RGB')
    assert ImageChops.difference(restored, corrected).getbbox() is None, 'Wallpaper must match the label-corrected original'
    difference = ImageChops.difference(old, restored).convert('L')
    assert difference.getbbox(), 'No prior trace recoloring was restored'
    for name, box in {'battery':(30,1060,1195,2300), 'left flex':(35,925,330,1060),
                      'right flex':(900,875,1120,1080), 'upper flex':(900,40,1080,200),
                      'camera flex':(465,110,650,235), 'chip label':(523,695,698,740)}.items():
        assert difference.crop(box).getbbox() is None, f'Restoration changed protected {name}'
    # Restoration may only undo the prior orange recolor regions and the
    # original user-drawn copper strokes (including antialiased edges).
    allowed = Image.new('L', restored.size, 0)
    draw = ImageDraw.Draw(allowed)
    for x0, y0, x1, y1 in [(0,110,465,235), (840,205,1090,875), (330,785,635,950),
                           (0,180,42,920), (0,920,15,2350), (1209,150,1224,2350), (0,2350,100,2450)]:
        draw.rectangle((x0,y0,x1-1,y1-1), fill=255)
    paths = json.loads(Path('assets/circuit-paths/Jalur-2.json').read_text())['paths']
    for path in paths[::2]:
        points = [tuple(point) for point in path['points']]
        radius = path['width'] / 2 + 4
        draw.line(points, fill=255, width=path['width'] + 8, joint='curve')
        for x, y in points:
            draw.ellipse((x-radius,y-radius,x+radius,y+radius), fill=255)
    assert ImageChops.multiply(difference, ImageChops.invert(allowed)).getbbox() is None, 'Restoration changed unrelated pixels'
    for x, y in [(1040,270), (1017,750), (470,944)]:
        assert restored.getpixel((x, y)) == corrected.getpixel((x, y))
        assert restored.getpixel((x, y)) != old.getpixel((x, y))
print('Original trace colors restored; unrelated pixels, labels, battery and cables preserved: OK')
