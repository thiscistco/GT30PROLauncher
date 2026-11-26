#!/usr/bin/env python3
"""Verify the reviewed calendar data and parity of both offline consumers."""
import json, datetime
from pathlib import Path

root = Path(__file__).resolve().parents[1]
raw = (root / 'assets/calendar/indonesia.json').read_text(encoding='utf-8')
data = json.loads(raw)
assert (root / 'app/src/main/assets/calendar_indonesia.json').read_text(encoding='utf-8') == raw
js = (root / 'preview/holiday-data.js').read_text(encoding='utf-8')
assert raw.rstrip() in js
assert data['years'] == [2026, 2027]
counts = {year: {'libur': 0, 'cuti': 0, 'peringatan': 0} for year in data['years']}
for key, entries in data['entries'].items():
    date = datetime.date.fromisoformat(key)
    assert date.year in data['years'] and entries
    for event in entries:
        assert event['type'] in counts[date.year] and event['name']
        counts[date.year][event['type']] += 1
assert counts == {2026: {'libur': 17, 'cuti': 8, 'peringatan': 10}, 2027: {'libur': 18, 'cuti': 8, 'peringatan': 10}}
for key, label, kind in [('2026-08-17', 'Kemerdekaan', 'libur'), ('2026-04-21', 'Kartini', 'peringatan'), ('2026-03-20', 'Idulfitri', 'cuti'), ('2027-03-10', 'Idulfitri', 'libur'), ('2027-12-26', 'Isra Mikraj', 'libur')]:
    assert any(event['type'] == kind and label in event['name'] for event in data['entries'][key])
print('Indonesia holiday dataset coverage, classifications and Android/preview parity: OK')
