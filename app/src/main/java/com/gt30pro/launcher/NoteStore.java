package com.gt30pro.launcher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

/** Private on-device SQLite store; migrations never discard old notes. */
final class NoteStore extends SQLiteOpenHelper {
    static final int ACTIVE=0, ARCHIVE=1, TRASH=2;
    static final class Note {
        final long id; final String title,body,tag; final long updated; final int pinned,state,previousState,color;
        Note(long id,String title,String body,long updated,int pinned,int state,int previousState,String tag,int color) {
            this.id=id; this.title=title; this.body=body; this.updated=updated; this.pinned=pinned; this.state=state; this.previousState=previousState; this.tag=tag; this.color=color;
        }
    }
    NoteStore(Context c) { super(c,"notes.db",null,2,db -> { }); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, updated INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, state INTEGER NOT NULL DEFAULT 0, previous_state INTEGER NOT NULL DEFAULT 0, tag TEXT NOT NULL DEFAULT '', color INTEGER NOT NULL DEFAULT 0)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        if(oldVersion==1 && newVersion==2) {
            db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE notes ADD COLUMN state INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE notes ADD COLUMN previous_state INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE notes ADD COLUMN tag TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT 0");
        } else throw new SQLiteException("Unsupported notes database version");
    }
    ArrayList<Note> list() {
        ArrayList<Note> notes=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("notes",new String[]{"_id","title","body","updated","pinned","state","previous_state","tag","color"},null,null,null,null,"pinned DESC, updated DESC, _id DESC")) {
            while(c.moveToNext()) notes.add(new Note(c.getLong(0),c.getString(1),c.getString(2),c.getLong(3),c.getInt(4),c.getInt(5),c.getInt(6),c.getString(7),c.getInt(8)));
        }
        return notes;
    }
    long save(long id,String title,String body) { return save(id,title,body,null); }
    long save(long id,String title,String body,String tag) {
        if(tag!=null && tag.length()>60) return -1;
        if(title==null || body==null || title.length()>NoteText.TITLE_LIMIT || body.length()>NoteText.BODY_LIMIT || (id==0 && !NoteText.valid(title,body))) return -1;
        ContentValues values=new ContentValues(); values.put("title",title); values.put("body",body); values.put("updated",System.currentTimeMillis());
        if(tag!=null) values.put("tag",tag);
        SQLiteDatabase db=getWritableDatabase();
        if(id==0) return db.insertOrThrow("notes",null,values);
        return db.update("notes",values,"_id=? AND state<>2",new String[]{Long.toString(id)})==1 ? id : -1;
    }
    boolean metadata(long id,int pinned,String tag,int color) {
        ContentValues v=new ContentValues(); v.put("pinned",pinned==0?0:1); v.put("tag",tag); v.put("color",color); v.put("updated",System.currentTimeMillis());
        return getWritableDatabase().update("notes",v,"_id=? AND state<>2",new String[]{Long.toString(id)})==1;
    }
    boolean archive(long id,boolean archived) {
        ContentValues v=new ContentValues(); v.put("state",archived?ARCHIVE:ACTIVE); v.put("updated",System.currentTimeMillis());
        return getWritableDatabase().update("notes",v,"_id=? AND state<>2",new String[]{Long.toString(id)})==1;
    }
    boolean trash(long id) {
        SQLiteDatabase db=getWritableDatabase();
        android.database.sqlite.SQLiteStatement stmt=db.compileStatement("UPDATE notes SET previous_state=state,state=2,updated=? WHERE _id=? AND state<>2");
        try { stmt.bindLong(1,System.currentTimeMillis()); stmt.bindLong(2,id); return stmt.executeUpdateDelete()==1; } finally { stmt.close(); }
    }
    boolean restore(long id) {
        android.database.sqlite.SQLiteStatement stmt=getWritableDatabase().compileStatement("UPDATE notes SET state=CASE WHEN previous_state=1 THEN 1 ELSE 0 END, previous_state=0,updated=? WHERE _id=? AND state=2");
        try { stmt.bindLong(1,System.currentTimeMillis()); stmt.bindLong(2,id); return stmt.executeUpdateDelete()==1; } finally { stmt.close(); }
    }
    /** Append copies atomically. Existing IDs and data are never overwritten. */
    void importCopies(ArrayList<Note> notes) {
        SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
        try {
            for(Note n:notes) {
                ContentValues v=new ContentValues(); v.put("title",n.title); v.put("body",n.body); v.put("updated",n.updated);
                v.put("pinned",n.pinned); v.put("state",n.state); v.put("previous_state",n.previousState); v.put("tag",n.tag); v.put("color",n.color);
                db.insertOrThrow("notes",null,v);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
    boolean deletePermanently(long id) { return getWritableDatabase().delete("notes","_id=? AND state=2",new String[]{Long.toString(id)})==1; }
}
