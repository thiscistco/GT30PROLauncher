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
    private static final String[][] DEFAULTS = {
        {"07:00","08:00",""}, {"08:00","08:40",""}, {"08:40","09:15",""}, {"09:15","09:45",""},
        {"09:45","10:30","Istirahat"}, {"10:30","11:10",""}, {"11:10","11:50",""}, {"11:50","12:50","ISHOMA"},
        {"12:50","13:30",""}, {"13:30","14:10",""}, {"14:10","14:50",""}, {"14:50","15:30",""}
    };
    static final class SeedResult {
        final JSONArray rows; final int skipped;
        SeedResult(JSONArray rows, int skipped) { this.rows=rows; this.skipped=skipped; }
    }
    // Insert missing non-conflicting rows once; never replace existing time slots or cells.
    static SeedResult seed(JSONArray saved) throws org.json.JSONException {
        JSONArray result=new JSONArray(saved.toString()); int skipped=0;
        for(String[] preset:DEFAULTS) {
            JSONObject exact=null;
            for(int i=0;i<result.length();i++) { JSONObject item=result.optJSONObject(i); if(item!=null && preset[0].equals(item.optString("start")) && preset[1].equals(item.optString("end"))) { exact=item; break; } }
            if(exact!=null) {
                // Add a break label only to a truly empty row; preserve existing entries and labels.
                JSONArray cells=exact.optJSONArray("cells"); boolean hasEntry=false;
                if(cells!=null) for(int c=0;c<cells.length();c++) { JSONObject cell=cells.optJSONObject(c); if(cell!=null && (!cell.optString("subject","").trim().isEmpty() || !cell.optString("class","").trim().isEmpty())) { hasEntry=true; break; } }
                if(!preset[2].isEmpty() && !hasEntry && exact.optString("label","").isEmpty()) exact.put("label",preset[2]);
                continue;
            }
            if(!validRange(preset[0],preset[1],result,-1)) { skipped++; continue; }
            JSONObject row=new JSONObject().put("start",preset[0]).put("end",preset[1]).put("cells",new JSONArray());
            if(!preset[2].isEmpty()) row.put("label",preset[2]); result.put(row);
        }
        return new SeedResult(sorted(result),skipped);
    }
    static final String[] COLOR_KEYS={"biru","hijau","kuning","merah","ungu","jingga"};
    static final int[] COLORS={0xFFBBDDF7,0xFFBFE9CE,0xFFF4E3A4,0xFFF6C7CA,0xFFDCCCF4,0xFFF6D4B4};
    private static String normalized(String value) { return value==null?"":value.trim().replaceAll("\\s+"," ").toLowerCase(java.util.Locale.ROOT); }
    static int colorIndex(String classroom,String subject,String selected,JSONArray rows) {
        for(int i=0;i<COLOR_KEYS.length;i++) if(COLOR_KEYS[i].equals(selected)) return i;
        String c=normalized(classroom), s=normalized(subject);
        if(!c.isEmpty() || !s.isEmpty()) for(int r=0;r<rows.length();r++) {
            JSONObject row=rows.optJSONObject(r); JSONArray cells=row==null?null:row.optJSONArray("cells");
            if(cells==null)continue;
            for(int d=0;d<cells.length();d++) { JSONObject cell=cells.optJSONObject(d); if(cell==null)continue;
                if(c.equals(normalized(cell.optString("class",""))) && s.equals(normalized(cell.optString("subject","")))) {
                    String color=cell.optString("color",""); for(int i=0;i<COLOR_KEYS.length;i++) if(COLOR_KEYS[i].equals(color)) return i;
                }
            }
        }
        return Integer.remainderUnsigned((c+"\u0000"+s).hashCode(),COLOR_KEYS.length);
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
