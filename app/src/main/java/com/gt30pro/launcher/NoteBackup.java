package com.gt30pro.launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Versioned, unencrypted, portable snapshot. Import validates everything before writing. */
final class NoteBackup {
    static final int MAX_BYTES=16*1024*1024, MAX_NOTES=5000;
    static byte[] encode(ArrayList<NoteStore.Note> notes) throws JSONException, IOException {
        if(notes.size()>MAX_NOTES) throw new IOException("Terlalu banyak catatan untuk satu cadangan");
        JSONArray records=new JSONArray(); long textBytes=0;
        for(NoteStore.Note n:notes) {
            textBytes+=(long)n.title.getBytes(StandardCharsets.UTF_8).length+n.body.getBytes(StandardCharsets.UTF_8).length+n.tag.getBytes(StandardCharsets.UTF_8).length;
            if(textBytes>MAX_BYTES) throw new IOException("Cadangan lebih dari 16 MB");
            JSONObject v=new JSONObject();
            v.put("title",n.title); v.put("body",n.body); v.put("updated",n.updated); v.put("pinned",n.pinned);
            v.put("state",n.state); v.put("previous_state",n.previousState); v.put("tag",n.tag); v.put("color",n.color);
            records.put(v);
        }
        JSONObject root=new JSONObject(); root.put("format","gt30-notes"); root.put("version",1); root.put("notes",records);
        byte[] data=root.toString().getBytes(StandardCharsets.UTF_8);
        if(data.length>MAX_BYTES) throw new IOException("Cadangan lebih dari 16 MB");
        return data;
    }
    static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int n,total=0;
        while((n=in.read(buffer))!=-1) { total+=n; if(total>MAX_BYTES) throw new IOException("Cadangan lebih dari 16 MB"); out.write(buffer,0,n); }
        try { return StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(out.toByteArray())).toString(); }
        catch(java.nio.charset.CharacterCodingException ex) { throw new IOException("Berkas bukan UTF-8 yang valid",ex); }
    }
    private static String string(JSONObject o,String key,int max) throws JSONException {
        Object v=o.get(key); if(!(v instanceof String) || ((String)v).length()>max) throw new JSONException("Teks tidak valid: "+key); return (String)v;
    }
    private static long number(JSONObject o,String key,long min,long max) throws JSONException {
        Object value=o.get(key); if(!(value instanceof Number)) throw new JSONException("Angka tidak valid: "+key);
        Number n=(Number)value; double d=n.doubleValue(); long v=n.longValue();
        if(!Double.isFinite(d) || d!=v || v<min || v>max) throw new JSONException("Angka di luar batas: "+key); return v;
    }
    static ArrayList<NoteStore.Note> decode(String data) throws JSONException {
        org.json.JSONTokener tokener=new org.json.JSONTokener(data); JSONObject root=new JSONObject(tokener);
        if(tokener.nextClean()!=0) throw new JSONException("Data tambahan setelah cadangan");
        if(!"gt30-notes".equals(string(root,"format",30)) || number(root,"version",1,1)!=1) throw new JSONException("Format cadangan tidak didukung");
        JSONArray records=root.getJSONArray("notes"); if(records.length()>MAX_NOTES) throw new JSONException("Terlalu banyak catatan");
        ArrayList<NoteStore.Note> out=new ArrayList<>();
        for(int i=0;i<records.length();i++) {
            JSONObject n=records.getJSONObject(i); String title=string(n,"title",NoteText.TITLE_LIMIT), body=string(n,"body",NoteText.BODY_LIMIT), tag=string(n,"tag",60);
            long updated=number(n,"updated",0,Long.MAX_VALUE); int pinned=(int)number(n,"pinned",0,1), state=(int)number(n,"state",0,2), previous=(int)number(n,"previous_state",0,1), color=(int)number(n,"color",0,4);
            out.add(new NoteStore.Note(0,title,body,updated,pinned,state,previous,tag,color));
        }
        return out;
    }
    private NoteBackup() {}
}
