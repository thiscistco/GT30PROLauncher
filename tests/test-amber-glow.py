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
for before, after in zip(original['paths'], config['paths']):
    assert all(before[k] == after[k] for k in ('kind', 'width', 'offset'))
    assert before['points'][0] == after['points'][0] and before['points'][-1] == after['points'][-1]
    # Current data needs no point removal; each point moves at most one source
    # pixel per axis, keeping even small turns close to the original trace.
    assert len(before['points']) == len(after['points'])
    for old_point, new_point in zip(before['points'], after['points']):
        assert max(abs(old_point[0] - new_point[0]), abs(old_point[1] - new_point[1])) <= 1
        changed += old_point != new_point
assert changed > 0, 'Smoothing did not change any points'
assert len(config['paths']) == 14 and all(path.get('width') == 8 for path in config['paths'])
for i in range(0, len(config['paths']), 2):
    assert config['paths'][i]['points'] == config['paths'][i + 1]['points']
    assert (config['paths'][i]['offset'], config['paths'][i + 1]['offset']) == (0, 0.5)
assert a.getbbox() is not None, 'User paths must have a glow mask'
print('User paths and glow alignment, shared assets and protected regions: OK')
