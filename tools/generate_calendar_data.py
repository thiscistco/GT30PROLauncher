#!/usr/bin/env python3
"""Sync the reviewed holiday dataset to Android and the offline browser preview."""
from pathlib import Path
import json

root = Path(__file__).resolve().parents[1]
source = (root / 'assets/calendar/indonesia.json').read_text(encoding='utf-8')
json.loads(source)
(root / 'app/src/main/assets/calendar_indonesia.json').write_text(source, encoding='utf-8')
(root / 'preview/holiday-data.js').write_text(
    '/* Generated from assets/calendar/indonesia.json; do not edit directly. */\n'
    '(function(root, data) { if (typeof module !== "undefined" && module.exports) module.exports = data; '
    'if (root) root.indonesiaHolidays = data; })(typeof window !== "undefined" ? window : null,\n'
    + source.rstrip() + '\n);\n', encoding='utf-8')
