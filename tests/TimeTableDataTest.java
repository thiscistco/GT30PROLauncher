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
    System.out.println("Time Table: validation, adjacent slots, sorting and cell preservation: OK");
  }
}
