#!/usr/bin/env python3
"""Check that recoloring is limited to the original orange areas. Requires Pillow."""
from pathlib import Path
import subprocess
import json
import sys
import tempfile
from PIL import Image, ImageChops, ImageDraw

preview = Path('preview/assets/gt30pro-internals.png')
assert preview.read_bytes() == Path('app/src/main/assets/wallpaper_gt30pro.png').read_bytes()
with tempfile.TemporaryDirectory() as temp:
    original = Path(temp) / 'corrected.png'
    generated = Path(temp) / 'copper.png'
    final = Path(temp) / 'traces.png'
    subprocess.run([sys.executable, 'tools/edit_chip_label.py', 'assets/wallpaper-source/sketch1795646565939.jpeg', str(original)], check=True)
    subprocess.run([sys.executable, 'tools/recolor_copper.py', str(original), str(generated)], check=True)
    # The static wallpaper remains based on the original user drawing;
    # only the animated paths and glow are smoothed.
    subprocess.run([sys.executable, 'tools/color_trace_copper.py', str(generated), 'assets/circuit-paths/Jalur-2.json', str(final)], check=True)
    before = Image.open(original).convert('RGB')
    intermediate = Image.open(generated).convert('RGB')
    after = Image.open(preview).convert('RGB')
    assert ImageChops.difference(after, Image.open(final).convert('RGB')).getbbox() is None, 'Recoloring is not reproducible'
    difference = ImageChops.difference(before, intermediate).convert('L')
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
        r1, g1, _ = intermediate.getpixel((x, y))
        assert r1 - g1 < (r0 - g0) / 2, f'Orange remains at {x},{y}'
    # The new edit is confined to the user's drawn paths, including the small
    # antialias margin. It must never affect battery, flex cables or chip text.
    trace_diff = ImageChops.difference(intermediate, after).convert('L')
    assert trace_diff.getbbox(), 'No changes were made along the user paths'
    paths = json.loads(Path('assets/circuit-paths/Jalur-2.json').read_text())['paths']
    allowed_trace = Image.new('L', after.size, 0)
    draw = ImageDraw.Draw(allowed_trace)
    for path in paths:
        points = [tuple(point) for point in path['points']]
        radius = path['width'] / 2 + 4
        draw.line(points, fill=255, width=path['width'] + 8, joint='curve')
        for x, y in points:
            draw.ellipse((x-radius, y-radius, x+radius, y+radius), fill=255)
    assert ImageChops.multiply(trace_diff, ImageChops.invert(allowed_trace)).getbbox() is None, 'Changed pixels away from user paths'
    for name, box in {'battery':(30,1060,1195,2300), 'left flex':(35,925,330,1060),
                      'right flex':(900,875,1120,1080), 'upper flex':(900,40,1080,200),
                      'camera flex':(465,110,650,235), 'chip label':(523,695,698,740)}.items():
        assert trace_diff.crop(box).getbbox() is None, f'Trace edit changed protected {name}'
    for path in paths[::2]:
        assert any(trace_diff.getpixel(tuple(p)) > 0 for p in path['points']), 'One user path was not recolored'
print('Copper recoloring is reproducible, restricted to paths and preserves protected regions: OK')
