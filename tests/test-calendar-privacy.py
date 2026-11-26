#!/usr/bin/env python3
"""The holiday feed and the user's device-only events must stay separate."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
app = manifest.find('application')
assert app.get(ns + 'fullBackupContent') == '@xml/backup_rules'
assert app.get(ns + 'dataExtractionRules') == '@xml/data_extraction_rules'
old = ET.parse(root / 'app/src/main/res/xml/backup_rules.xml').getroot()
assert any(x.get('domain') == 'sharedpref' and x.get('path') == 'launcher_calendar.xml' for x in old.findall('exclude'))
new = ET.parse(root / 'app/src/main/res/xml/data_extraction_rules.xml').getroot()
for section in ('cloud-backup', 'device-transfer'):
    assert any(x.get('domain') == 'sharedpref' and x.get('path') == 'launcher_calendar.xml' for x in new.find(section).findall('exclude'))
calendar = (root / 'app/src/main/java/com/gt30pro/launcher/CalendarActivity.java').read_text()
feed = (root / 'app/src/main/java/com/gt30pro/launcher/HolidayFeed.java').read_text()
assert 'getSharedPreferences("launcher_calendar", MODE_PRIVATE)' in calendar
assert 'getSharedPreferences("launcher_holidays", MODE_PRIVATE)' in calendar
assert 'launcher_calendar' not in feed and 'events_v1' not in feed
assert 'setInstanceFollowRedirects(false)' in feed
assert 'api.github.com/repos/thiscistco/GT30PROLauncher/' in feed
print('Calendar events local-only, backup exclusion and feed separation: OK')
