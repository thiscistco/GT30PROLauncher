package com.gt30pro.launcher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

/** Format runs refer to UTF-16 offsets in the plain-text body. */
final class NoteFormats {
    static final int MAX_RUNS=2000;
    static final class Run {
        final int start,end; final String type;
        Run(int start,int end,String type) { this.start=start; this.end=end; this.type=type; }
    }
    static ArrayList<Run> read(String json,int length) throws JSONException {
        ArrayList<Run> out=new ArrayList<>(); if(json==null) return out;
        JSONArray array=new JSONArray(json); if(array.length()>MAX_RUNS) throw new JSONException("Terlalu banyak format");
        for(int i=0;i<array.length();i++) {
            JSONObject item=array.getJSONObject(i); Object a=item.get("start"), b=item.get("end"), t=item.get("type");
            if(!(a instanceof Number) || !(b instanceof Number) || !(t instanceof String)) throw new JSONException("Format tidak valid");
            double x=((Number)a).doubleValue(),y=((Number)b).doubleValue(); int start=((Number)a).intValue(),end=((Number)b).intValue();
            if(x!=start || y!=end || start<0 || start>=end || end>length || !(t.equals("bold")||t.equals("italic")||t.equals("underline"))) throw new JSONException("Rentang format tidak valid");
            out.add(new Run(start,end,(String)t));
        }
        return out;
    }
    static String write(ArrayList<Run> runs,int length) throws JSONException {
        JSONArray arr=new JSONArray(); for(Run run:runs) { JSONObject item=new JSONObject(); item.put("start",run.start); item.put("end",run.end); item.put("type",run.type); arr.put(item); }
        String json=arr.toString(); read(json,length); return json;
    }
    private NoteFormats() {}
}
