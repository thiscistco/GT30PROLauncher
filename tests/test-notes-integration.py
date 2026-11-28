#!/usr/bin/env python3
"""Native wiring, data privacy and safe editor state checks; not an Android device test."""
from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/com/gt30pro/launcher'
activity=(base/'NotesActivity.java').read_text(); store=(base/'NoteStore.java').read_text()
assert 'extends SwipeBackActivity' in activity and 'ui.header(this,font,"Catatan")' in activity
assert 'onSaveInstanceState' in activity and 'lastTitle' in activity and 'lastBody' in activity
assert 'InputFilter.LengthFilter(limit)' in activity and 'NoteText.valid(t,b)' in activity
assert 'postDelayed(autoSave,600)' in activity and 'onPause() { if(editing) saveDraft(false)' in activity
assert 'setTitle("Hapus permanen?")' in activity and 'Catatan dapat dipulihkan dari Sampah.' in activity
assert 'NoteSearch.matches(n.title,n.body,tagDisplay(n),query)' in activity and 'ACTION_SEND' in activity
assert 'ACTION_CREATE_DOCUMENT' in activity and 'NoteExport.writePdf' in activity and 'NoteExport.writeText' in activity
assert 'supportsBottomSwipe() { return !editing; }' in activity
assert 'SQLiteOpenHelper' in store and 'insertOrThrow' in store
assert 'db.update("notes",values,"_id=? AND state<>2"' in store and 'delete("notes","_id=? AND state=2"' in store
assert 'db -> { }' in store and 'DROP TABLE' not in store
assert 'SQLiteException' in activity and 'teks masih di editor' in activity
manifest=ET.parse(root/'app/src/main/AndroidManifest.xml').getroot(); ns='{http://schemas.android.com/apk/res/android}'
assert any(a.get(ns+'name')=='.NotesActivity' and a.get(ns+'exported')=='false' for a in manifest.find('application').findall('activity'))
for path in ('notes.db','notes.db-wal','notes.db-shm','notes.db-journal'):
    full=ET.parse(root/'app/src/main/res/xml/backup_rules.xml').getroot()
    assert any(x.get('domain')=='database' and x.get('path')==path for x in full.findall('exclude'))
    rules=ET.parse(root/'app/src/main/res/xml/data_extraction_rules.xml').getroot()
    for section in ('cloud-backup','device-transfer'):
        assert any(x.get('domain')=='database' and x.get('path')==path for x in rules.find(section).findall('exclude'))
print('Notes: local storage, safe editor state, error handling and backup exclusion wired: OK')
reminders=(base/'NoteReminders.java').read_text(); restore=(base/'ClockRestoreReceiver.java').read_text()
assert 'NoteReminders.restore(context)' in restore and 'setExactAndAllowWhileIdle' in reminders and 'setAndAllowWhileIdle' in reminders
assert '.setContentText("Buka Catatan untuk melihat pengingat.")' in reminders and 'note_id' in reminders
assert any(r.get(ns+'name')=='.NoteReminderReceiver' and r.get(ns+'exported')=='false' for r in manifest.find('application').findall('receiver'))
assert 'NoteReminders.cancel(this,note.id)' in activity and 'ACTION_OPEN_DOCUMENT' in activity
assert 'db.beginTransaction()' in store and 'db.setTransactionSuccessful()' in store
for file in ('backup_rules.xml','data_extraction_rules.xml'):
    assert 'path="note_reminders.xml"' in (root/'app/src/main/res/xml'/file).read_text()
print('Notes reminder and backup wiring OK (device behavior requires Android testing)')

# Editor tag changes are part of the same SQLite save and restored with unsaved drafts.
assert 'store.saveWithTags(editId,t,b,draftTag,format)' in activity and 'values.put("tags",tags)' in store
assert 'out.putString("lastTag",lastTag)' in activity and 'out.putString("draftTag",draftTag)' in activity
assert 'available.addAll(selected)' in activity and 'Buat tag' in activity
for description in ('Lokasi: ','Tampilan: ','Catatan baru','Pilih atau buat tag','Tambah item checklist','Selesai'):
    assert description in activity
print('Tag editing and accessible icon wiring OK')
