#!/usr/bin/env python3
"""Execute the actual SQL schema/migration/transition strings against SQLite."""
from pathlib import Path
import re,sqlite3
src=(Path(__file__).resolve().parents[1]/'app/src/main/java/com/gt30pro/launcher/NoteStore.java').read_text()
schema=re.search(r'db\.execSQL\("(CREATE TABLE notes [^\"]+)"\)',src).group(1)
alter=re.findall(r'db\.execSQL\("(ALTER TABLE notes [^\"]+)"\)',src)
assert len(alter)==7
old=sqlite3.connect(':memory:'); old.execute('CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, updated INTEGER NOT NULL)')
old.execute('INSERT INTO notes (title,body,updated) VALUES (?,?,?)',('asli','isi lama',123)); old.commit()
with old:
 for sql in alter: old.execute(sql)
row=old.execute('SELECT _id,title,body,updated,pinned,state,previous_state,tag,color,tags,format FROM notes').fetchone()
assert row==(1,'asli','isi lama',123,0,0,0,'',0,None,None)
new=sqlite3.connect(':memory:'); new.execute(schema)
assert [x[1] for x in old.execute('PRAGMA table_info(notes)')]==[x[1] for x in new.execute('PRAGMA table_info(notes)')]
trash=re.search(r'compileStatement\("(UPDATE notes SET previous_state=state[^\"]+)"\)',src).group(1)
restore=re.search(r'compileStatement\("(UPDATE notes SET state=CASE[^\"]+)"\)',src).group(1)
old.execute('UPDATE notes SET state=1 WHERE _id=1'); old.execute(trash,(124,1)); assert old.execute('SELECT state,previous_state FROM notes').fetchone()==(2,1)
old.execute(restore,(125,1)); assert old.execute('SELECT state,previous_state FROM notes').fetchone()==(1,0)
old.execute('UPDATE notes SET state=0 WHERE _id=1'); old.execute(trash,(126,1)); old.execute(restore,(127,1)); assert old.execute('SELECT state,previous_state FROM notes').fetchone()==(0,0)
assert old.execute('SELECT title,body FROM notes').fetchone()==('asli','isi lama')
print('Notes database: v1-to-v3 migration and active/archive trash restoration OK')
old2=sqlite3.connect(':memory:'); old2.execute("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, updated INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, state INTEGER NOT NULL DEFAULT 0, previous_state INTEGER NOT NULL DEFAULT 0, tag TEXT NOT NULL DEFAULT '', color INTEGER NOT NULL DEFAULT 0)"); old2.execute("INSERT INTO notes(title,body,updated,tag) VALUES('x','y',1,'asli')"); [old2.execute(sql) for sql in alter[-2:]]; assert old2.execute('SELECT tag,tags,format FROM notes').fetchone()==('asli',None,None)
# Import is append-only and rolls back the entire batch on a database failure.
assert 'db.beginTransaction()' in src and 'db.setTransactionSuccessful()' in src and 'db.endTransaction()' in src
assert 'db.insertOrThrow("notes",null,v)' in src
before=old.execute('SELECT COUNT(*) FROM notes').fetchone()[0]; old.commit()
try:
 with old:
  old.execute('INSERT INTO notes (title,body,updated) VALUES (?,?,?)',('copy','safe',123))
  old.execute('INSERT INTO notes (title,body,updated) VALUES (?,?,?)',(None,'invalid',123))
except sqlite3.IntegrityError: pass
assert old.execute('SELECT COUNT(*) FROM notes').fetchone()[0]==before
print('Import transaction rollback preserves existing notes OK')
