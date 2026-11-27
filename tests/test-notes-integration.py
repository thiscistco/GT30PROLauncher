#!/usr/bin/env python3
"""Native wiring, data privacy and safe editor state checks; not an Android device test."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/com/gt30pro/launcher'
activity=(base/'NotesActivity.java').read_text(); store=(base/'NoteStore.java').read_text()
assert 'extends SwipeBackActivity' in activity and 'ui.header(this,font,"Catatan")' in activity
assert 'onSaveInstanceState' in activity and 'originalTitle' in activity and 'originalBody' in activity
assert 'InputFilter.LengthFilter(limit)' in activity and 'NoteText.valid(t,b)' in activity
assert 'setTitle("Buang perubahan?")' in activity and 'setTitle("Hapus catatan?")' in activity
assert 'supportsBottomSwipe() { return !editing; }' in activity
assert 'SQLiteOpenHelper' in store and 'insertOrThrow' in store
assert 'db.update("notes",values,"_id=?"' in store and 'delete("notes","_id=?"' in store
assert 'db -> { }' in store and 'DROP TABLE' not in store
assert 'SQLiteException' in activity and 'Perubahan masih di editor' in activity
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot(); ns='{http://schemas.android.com/apk/res/android}'
assert any(a.get(ns+'name')=='.NotesActivity' and a.get(ns+'exported')=='false' for a in manifest.find('application').findall('activity'))
for path in ('notes.db','notes.db-wal','notes.db-shm','notes.db-journal'):
    full=ET.parse(root/'app/src/main/res/xml/backup_rules.xml').getroot()
    assert any(x.get('domain')=='database' and x.get('path')==path for x in full.findall('exclude'))
    rules=ET.parse(root/'app/src/main/res/xml/data_extraction_rules.xml').getroot()
    for section in ('cloud-backup','device-transfer'):
        assert any(x.get('domain')=='database' and x.get('path')==path for x in rules.find(section).findall('exclude'))
print('Notes: local storage, safe editor state, error handling and backup exclusion wired: OK')
