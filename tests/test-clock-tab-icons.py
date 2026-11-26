#!/usr/bin/env python3
"""Ensure Android vectors and preview SVGs use the same Doto-like dot grids."""
import importlib.util
from pathlib import Path
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('clock_icons', 'tools/generate_clock_tab_icons.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
assert list(module.ICONS) == ['alarm', 'world', 'timer', 'stopwatch']
assert len(set(tuple(rows) for rows in module.ICONS.values())) == 4
for path, expected in module.generate():
    assert path.read_text(encoding='utf-8') == expected, f'Generated icon is stale: {path}'
    root = ET.parse(path).getroot()
    name = path.stem.removeprefix('clock-').removeprefix('ic_clock_')
    dots = sum(row.count('#') for row in module.ICONS[name])
    if path.suffix == '.svg':
        assert len(list(root)) == dots
    else:
        data = list(root)[0].attrib['{http://schemas.android.com/apk/res/android}pathData']
        assert data.count('M') == dots
print('Android and preview clock icons share the same four dot grids: OK')
