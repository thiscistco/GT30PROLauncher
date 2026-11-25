#!/usr/bin/env python3
"""Validate overlay geometry and protected wallpaper regions. Requires Pillow."""
from pathlib import Path
import json
import math
from PIL import Image

preview = Path('preview/assets/wallpaper-amber-glow.png').read_bytes()
android = Path('app/src/main/assets/wallpaper_amber_glow.png').read_bytes()
assert preview == android, 'Preview and Android glow layers differ'
image = Image.open('preview/assets/wallpaper-amber-glow.png')
assert image.size == (1224, 2720) and image.mode == 'RGBA'
a = image.getchannel('A')
for name, bounds in {
    'battery': (30, 1060, 1195, 2300),
    'left flex': (35, 925, 330, 1060),
    'right flex': (900, 875, 1120, 1080),
    'upper flex': (900, 40, 1080, 200),
}.items():
    assert a.crop(bounds).getbbox() is None, f'Overlay touches protected {name}'
for name, bounds in {'top left': (0, 110, 375, 235), 'upper right': (840, 205, 1090, 875), 'lower trace': (330, 785, 635, 950)}.items():
    assert a.crop(bounds).getbbox() is not None, f'No glow in {name}'
config_data = Path('preview/assets/circuit-paths.json').read_bytes()
assert config_data == Path('app/src/main/assets/circuit_paths.json').read_bytes()
config = json.loads(config_data)
assert (config['width'], config['height']) == image.size
assert config['durationMs'] > 0 and config['radius'] > 0
for path in config['paths']:
    points = path['points']
    lengths = [math.dist(p, q) for p, q in zip(points, points[1:])]
    assert len(lengths) >= 1 and all(length > 0 for length in lengths)
    total = sum(lengths)
    for step in range(48):
        distance = total * step / 48
        for p, q, length in zip(points, points[1:], lengths):
            if distance <= length:
                x = round(p[0] + (q[0] - p[0]) * distance / length)
                y = round(p[1] + (q[1] - p[1]) * distance / length)
                break
            distance -= length
        nearby = a.crop((max(0, x - 50), max(0, y - 50), min(image.width, x + 51), min(image.height, y + 51)))
        assert nearby.getextrema()[1] >= 10, f'Path is detached from glow mask at {x}, {y}'
print('Amber moving paths, alignment and protected regions: OK')
