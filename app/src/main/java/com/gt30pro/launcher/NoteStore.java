package com.gt30pro.launcher;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

/** Private on-device SQLite store. Never delete a damaged database automatically. */
final class NoteStore extends SQLiteOpenHelper {
    static final class Note {
        final long id; final String title,body; final long updated;
        Note(long id,String title,String body,long updated) { this.id=id; this.title=title; this.body=body; this.updated=updated; }
    }
    NoteStore(Context c) { super(c,"notes.db",null,1,db -> { }); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, body TEXT NOT NULL, updated INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { throw new SQLiteException("Unsupported notes database version"); }
    ArrayList<Note> list() {
        ArrayList<Note> notes=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("notes",new String[]{"_id","title","body","updated"},null,null,null,null,"updated DESC, _id DESC")) {
            while(c.moveToNext()) notes.add(new Note(c.getLong(0),c.getString(1),c.getString(2),c.getLong(3)));
        }
        return notes;
    }
    boolean save(long id,String title,String body) {
        if(!NoteText.valid(title,body)) return false;
        ContentValues values=new ContentValues(); values.put("title",title); values.put("body",body); values.put("updated",System.currentTimeMillis());
        SQLiteDatabase db=getWritableDatabase();
        return id==0 ? db.insertOrThrow("notes",null,values)>0 : db.update("notes",values,"_id=?",new String[]{Long.toString(id)})==1;
    }
    boolean delete(long id) { return getWritableDatabase().delete("notes","_id=?",new String[]{Long.toString(id)})==1; }
}
