package com.gt30pro.launcher;
import org.json.JSONArray;
import org.json.JSONObject;
public class TimeTableDataTest {
  static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
  public static void main(String[] args) throws Exception {
    JSONArray rows=new JSONArray(); JSONObject later=new JSONObject().put("start","09:00").put("end","10:00").put("cells",new JSONArray().put(new JSONObject().put("subject","Matematika")));
    JSONObject earlier=new JSONObject().put("start","07:00").put("end","08:00");rows.put(later).put(earlier);
    check(TimeTableData.minutes("24:00")==-1 && TimeTableData.minutes("7:00")==-1 && TimeTableData.minutes("23:59")==1439,"strict 24-hour time");
    check(!TimeTableData.validRange("07:30","08:30",rows,-1),"overlap rejected");
    check(TimeTableData.validRange("08:00","09:00",rows,-1),"adjacent accepted");
    check(!TimeTableData.validRange("11:00","10:00",rows,-1),"reverse range rejected");
    check(TimeTableData.validRange("09:15","10:15",rows,0),"editing ignores itself");
    JSONArray sorted=TimeTableData.sorted(rows); check(sorted.optJSONObject(0)==earlier && sorted.optJSONObject(1)==later,"chronological with cells attached");
    check(sorted.optJSONObject(1).optJSONArray("cells").optJSONObject(0).optString("subject").equals("Matematika"),"contents preserved");
    check(rows.optJSONObject(0)==later,"input unmodified");
    TimeTableData.SeedResult defaults=TimeTableData.seed(new JSONArray());
    check(defaults.rows.length()==12 && defaults.skipped==0,"twelve defaults");
    String[][] expected={{"07:00","08:00"},{"08:00","08:40"},{"08:40","09:15"},{"09:15","09:45"},{"09:45","10:30"},{"10:30","11:10"},{"11:10","11:50"},{"11:50","12:50"},{"12:50","13:30"},{"13:30","14:10"},{"14:10","14:50"},{"14:50","15:30"}};
    for(int i=0;i<12;i++) check(defaults.rows.optJSONObject(i).optString("start").equals(expected[i][0]) && defaults.rows.optJSONObject(i).optString("end").equals(expected[i][1]),"preset "+i);
    check(defaults.rows.optJSONObject(4).optString("label").equals("Istirahat") && defaults.rows.optJSONObject(7).optString("label").equals("ISHOMA"),"breaks");
    JSONArray old=new JSONArray().put(new JSONObject().put("start","07:00").put("end","08:00").put("cells",new JSONArray().put(new JSONObject().put("subject","Matematika")))).put(new JSONObject().put("start","08:30").put("end","09:00").put("cells",new JSONArray().put(new JSONObject().put("class","X"))));
    TimeTableData.SeedResult migrated=TimeTableData.seed(old);check(migrated.skipped==2,"overlaps skipped");check(old.length()==2 && migrated.rows.optJSONObject(0).optJSONArray("cells").optJSONObject(0).optString("subject").equals("Matematika"),"old entries preserved");
    JSONArray oldBreak=new JSONArray().put(new JSONObject().put("start","09:45").put("end","10:30").put("cells",new JSONArray()));
    check(TimeTableData.seed(oldBreak).rows.optJSONObject(4).optString("label").equals("Istirahat"),"empty exact break labeled");
    check(!oldBreak.optJSONObject(0).has("label"),"seed does not mutate source");
    System.out.println("Time Table: validation, adjacent slots, sorting and cell preservation: OK");
  }
}
