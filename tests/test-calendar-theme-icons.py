#!/usr/bin/env python3
"""Calendar icons stay reproducible, consistent across Android and preview."""
import importlib.util
from pathlib import Path
root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('icons', root / 'tools/generate_calendar_theme_icons.py')
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
for path, expected in mod.generate():
    assert path.read_text(encoding='utf8') == expected, f'{path} is stale'
java = (root / 'app/src/main/java/com/gt30pro/launcher/WeatherIcons.java').read_text()
for name, symbol in [('light', 'SUN'), ('dark', 'MOON')]:
    expected = ','.join('"' + row + '"' for row in mod.ICONS[name])
    assert f'String[] {symbol} = {{{expected}}}' in java
print('Calendar theme dot icons: reproducible, Android/preview parity and Doto style: OK')
