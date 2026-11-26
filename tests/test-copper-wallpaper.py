#!/usr/bin/env python3
"""Check gunmetal circuits, neutralized orange halos, and protected regions."""
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
    neutral = Path(temp) / 'neutral.png'
    expected = Path(temp) / 'traces.png'
    subprocess.run([sys.executable, 'tools/edit_chip_label.py', 'assets/wallpaper-source/sketch1795646565939.jpeg', str(original)], check=True)
    subprocess.run([sys.executable, 'tools/recolor_gunmetal.py', str(original), 'preview/assets/circuit-paths.json', str(neutral)], check=True)
    subprocess.run([sys.executable, 'tools/color_trace_gunmetal.py', str(neutral), 'preview/assets/circuit-paths.json', str(expected)], check=True)
    before = Image.open(original).convert('RGB')
    intermediate = Image.open(neutral).convert('RGB')
    after = Image.open(preview).convert('RGB')
    assert ImageChops.difference(after, Image.open(expected).convert('RGB')).getbbox() is None, 'Gunmetal wallpaper must be reproducible'
    difference = ImageChops.difference(before, after).convert('L')
    assert difference.getbbox(), 'No recoloring detected'
    for name, box in {'battery':(30,1060,1195,2300), 'left flex':(35,925,330,1060),
                      'right flex':(900,875,1120,1080), 'upper flex':(900,40,1080,200),
                      'camera flex':(465,110,650,235), 'chip label':(523,695,698,740)}.items():
        assert difference.crop(box).getbbox() is None, f'Recoloring changed protected {name}'
    # Orange neutralization is restricted to feathered circuit regions.
    regions = [(0,110,465,235), (840,205,1090,875), (330,785,700,995),
               (0,180,42,920), (0,920,15,2350), (1209,150,1224,2350), (0,2350,100,2450)]
    allowed = Image.new('L', after.size, 0)
    draw = ImageDraw.Draw(allowed)
    for x0,y0,x1,y1 in regions:
        draw.rectangle((x0,y0,x1-1,y1-1), fill=255)
    allowed = allowed.filter(ImageFilter.GaussianBlur(16))
    # Warm copper next to each route can also be neutralized, with a soft
    # boundary. The static trace itself remains within that band.
    near = Image.new('L', after.size, 0)
    draw = ImageDraw.Draw(near)
    paths = json.loads(Path('preview/assets/circuit-paths.json').read_text())['paths']
    for path in paths[::3]:
        points = [tuple(point) for point in path['points']]
        band = path['width'] + 24
        draw.line(points, fill=255, width=band, joint='curve')
        for x,y in points:
            draw.ellipse((x-band/2,y-band/2,x+band/2,y+band/2), fill=255)
        x,y = points[0]
        r,g,b = intermediate.getpixel((x,y))
        lum = .299*r + .587*g + .114*b
        assert after.getpixel((x,y)) == tuple(round(lum*c) for c in (.72,.82,.88))
    allowed = ImageChops.lighter(allowed, near.filter(ImageFilter.GaussianBlur(2)))
    allowed = allowed.point(lambda v: 255 if v else 0)
    assert ImageChops.multiply(difference, ImageChops.invert(allowed)).getbbox() is None, 'Changed pixels away from circuit regions'
    for point in [(1040,270),(1017,750),(470,944)]:
        r0,g0,b0 = before.getpixel(point)
        r,g,b = after.getpixel(point)
        assert r0-g0 > 35 and r <= g <= b, f'Orange circuit remains at {point}'
print('Gunmetal wallpaper and orange halo cleanup preserve unrelated and protected pixels: OK')
