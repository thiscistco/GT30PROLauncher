#!/usr/bin/env python3
"""Keep preview and Android calendar controls in sync and icon-only."""
import importlib.util
from pathlib import Path
root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('controls', root/'tools/generate_calendar_controls.py')
mod = importlib.util.module_from_spec(spec); spec.loader.exec_module(mod)
for path, data in mod.generate(): assert path.read_text(encoding='utf8') == data, f'{path} stale'
html = (root/'preview/index.html').read_text()
for name in mod.ICONS:
 assert f'calendar-{name}.svg' in html
assert 'calendar-day.marker-holiday::after' not in html
assert 'calendar-day.marker-observance::after' not in html
print('Calendar controls: reproducible dot icons and no dot markers: OK')
