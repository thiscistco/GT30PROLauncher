#!/usr/bin/env python3
"""Ensure generated white module icons are up to date and share a dot grid."""
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location('module_icons', Path('tools/generate_module_icons.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
assert list(module.ICONS) == ['clock', 'timetable', 'settings', 'calendar']
assert module.ICONS['clock'] != module.ICONS['calendar']
for path, expected in module.generate():
    assert path.read_text(encoding='utf-8') == expected, f'Generated icon is stale: {path}'
print('Generated Super App icons are reproducible: OK')
