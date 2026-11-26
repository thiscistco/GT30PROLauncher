#!/usr/bin/env python3
"""Verify exported path widths can generate Android masks without touching protected areas."""
import json
import subprocess
import sys
import tempfile
from pathlib import Path
from PIL import Image

with tempfile.TemporaryDirectory() as temp:
    temp = Path(temp)
    config = {'width': 1224, 'height': 2720, 'durationMs': 6000, 'radius': 100,
              'paths': [{'kind': 'copper', 'offset': 0, 'points': [[500, 300], [650, 300]]},
                        {'kind': 'copper', 'offset': 0, 'points': [[500, 1100], [650, 1100]]}]}
    masks = []
    for width in (4, 60):
        for path in config['paths']:
            path['width'] = width
        cfgfile = temp / 'paths.json'
        cfgfile.write_text(json.dumps(config))
        output = temp / f'mask-{width}.png'
        subprocess.run([sys.executable, 'tools/create_amber_glow.py',
                        'preview/assets/gt30pro-internals.png', str(output), str(cfgfile)], check=True)
        masks.append(Image.open(output).convert('RGBA').getchannel('A'))
    assert masks[1].getpixel((575, 320)) > masks[0].getpixel((575, 320)) + 50, 'Path width did not change the mask'
    assert all(mask.crop((30, 1060, 1195, 2300)).getbbox() is None for mask in masks), 'Custom mask touches battery'
print('Exported path widths generate distinct protected Android masks: OK')
