package com.gt30pro.launcher;

import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;

/** v3 tags are JSON arrays; null means an untouched v1/v2 single tag. */
final class NoteTags {
    static final int MAX_COUNT=20, MAX_LENGTH=60;
    static ArrayList<String> read(String json,String legacy) throws JSONException {
        ArrayList<String> out=new ArrayList<>();
        if(json==null) { if(legacy!=null && !legacy.isEmpty()) out.add(legacy); return out; }
        JSONArray array=new JSONArray(json);
        if(array.length()>MAX_COUNT) throw new JSONException("Terlalu banyak tag");
        for(int i=0;i<array.length();i++) {
            Object value=array.get(i); if(!(value instanceof String)) throw new JSONException("Tag bukan teks");
            String tag=(String)value;
            if(tag.trim().isEmpty() || !tag.equals(tag.trim()) || tag.length()>MAX_LENGTH) throw new JSONException("Tag tidak valid");
            for(String previous:out) if(previous.toLowerCase(Locale.ROOT).equals(tag.toLowerCase(Locale.ROOT))) throw new JSONException("Tag duplikat");
            out.add(tag);
        }
        return out;
    }
    static String write(ArrayList<String> tags) throws JSONException {
        JSONArray a=new JSONArray(); for(String tag:tags) a.put(tag); String value=a.toString(); read(value,""); return value;
    }
    static String display(ArrayList<String> tags) { StringBuilder b=new StringBuilder(); for(String tag:tags){ if(b.length()>0)b.append("  "); b.append('#').append(tag); } return b.toString(); }
    private NoteTags() {}
}
