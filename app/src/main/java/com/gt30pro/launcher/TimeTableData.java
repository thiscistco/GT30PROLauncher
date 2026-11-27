package com.gt30pro.launcher;

import org.json.JSONArray;
import org.json.JSONObject;

/** Local weekly timetable rows; each row carries its five cells when reordered. */
final class TimeTableData {
    static int minutes(String time) {
        if (time == null || !time.matches("[0-9]{2}:[0-9]{2}")) return -1;
        int h=Integer.parseInt(time.substring(0,2)), m=Integer.parseInt(time.substring(3,5));
        return h < 24 && m < 60 ? h*60+m : -1;
    }
    static boolean validRange(String start, String end, JSONArray rows, int edited) {
        int a=minutes(start), b=minutes(end);
        if (a < 0 || b <= a) return false;
        for (int i=0;i<rows.length();i++) {
            if (i == edited) continue;
            JSONObject row=rows.optJSONObject(i); if (row == null) continue;
            int x=minutes(row.optString("start")), y=minutes(row.optString("end"));
            if (x >= 0 && y > x && a < y && x < b) return false;
        }
        return true;
    }
    static JSONArray sorted(JSONArray rows) {
        java.util.ArrayList<JSONObject> list=new java.util.ArrayList<>();
        for (int i=0;i<rows.length();i++) { JSONObject row=rows.optJSONObject(i); if(row!=null) list.add(row); }
        java.util.Collections.sort(list,new java.util.Comparator<JSONObject>() {
            @Override public int compare(JSONObject a,JSONObject b) { return a.optString("start").compareTo(b.optString("start")); }
        });
        JSONArray result=new JSONArray(); for(JSONObject row:list) result.put(row); return result;
    }
}
