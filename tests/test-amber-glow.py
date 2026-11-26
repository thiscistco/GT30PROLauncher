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
# Every visible animation pixel must be cool gunmetal, never orange.
from PIL import ImageChops
red, green, blue, _ = image.split()
visible = a.point(lambda value: 255 if value else 0)
assert ImageChops.multiply(ImageChops.subtract(red, green), visible).getbbox() is None
assert ImageChops.multiply(ImageChops.subtract(green, blue), visible).getbbox() is None
for name, bounds in {
    'battery': (30, 1060, 1195, 2300),
    'left flex': (35, 925, 330, 1060),
    'right flex': (900, 875, 1120, 1080),
    'upper flex': (900, 40, 1080, 200),
}.items():
    assert a.crop(bounds).getbbox() is None, f'Overlay touches protected {name}'
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
        radius = 5 if path.get('kind') == 'copper' else 50
        nearby = a.crop((max(0, x - radius), max(0, y - radius), min(image.width, x + radius + 1), min(image.height, y + radius + 1)))
        assert nearby.getextrema()[1] >= 10, f'Path is detached from glow mask at {x}, {y}'
# Preserve the supplied drawing exactly while bundling a gentle derivative.
import hashlib
import subprocess
import sys
import tempfile
original_data = Path('assets/circuit-paths/Jalur-2.json').read_bytes()
assert hashlib.sha256(original_data).hexdigest() == '19ca606070b391edb80c022c88c7dcce7fc079fd6ae2f8d8be79dfbc8219c5fd'
original = json.loads(original_data)
assert all(config[key] == original[key] for key in ('width', 'height', 'durationMs', 'radius'))
with tempfile.TemporaryDirectory() as temp:
    output = Path(temp) / 'smooth.json'
    subprocess.run([sys.executable, 'tools/smooth_circuit_paths.py', 'assets/circuit-paths/Jalur-2.json', str(output)], check=True)
    assert output.read_bytes() == config_data, 'Smooth paths must be reproducible'
changed = 0
assert len(config['paths']) == 21 and all(path.get('width') == 8 for path in config['paths'])
for index, before in enumerate(original['paths'][::2]):
    first, middle, opposing = config['paths'][index * 3:index * 3 + 3]
    assert [first['offset'], middle['offset'], opposing['offset']] == [0, 0.5, 1]
    assert all(p['points'] == first['points'] and p['kind'] == before['kind'] and p['width'] == before['width'] for p in (first, middle, opposing))
    assert before['points'][0] == first['points'][0] and before['points'][-1] == first['points'][-1]
    # Averaging plus simplification must remain near the original route.
    def distance_to_segment(p, x, y):
        dx, dy = y[0] - x[0], y[1] - x[1]
        length2 = dx*dx + dy*dy
        t = max(0, min(1, ((p[0]-x[0])*dx + (p[1]-x[1])*dy) / length2)) if length2 else 0
        return math.hypot(p[0]-x[0]-t*dx, p[1]-x[1]-t*dy)
    segments = list(zip(first['points'], first['points'][1:]))
    for old_point in before['points']:
        assert min(distance_to_segment(old_point, x, y) for x, y in segments) <= 4
    assert len(first['points']) <= len(before['points'])
    changed += before['points'] != first['points']
assert changed > 0, 'Smoothing did not change any points'
assert a.getbbox() is not None, 'User paths must have a glow mask'
print('User paths and glow alignment, shared assets and protected regions: OK')
