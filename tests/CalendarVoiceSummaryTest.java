package com.gt30pro.launcher;
import java.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONObject;
public final class CalendarVoiceSummaryTest {
    static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    static JSONObject legacy(String title,String date,String time) throws Exception { return new JSONObject().put("id",title).put("title",title).put("date",date).put("time",time); }
    public static void main(String[] args) throws Exception {
        LocalDate today=LocalDate.parse("2026-12-30"); // Wednesday, week crosses year
        check(CalendarVoiceSummary.summarize(new JSONArray(),today).startsWith("Tidak ada acara"),"empty");
        JSONArray items=new JSONArray().put(legacy("Senin lalu","2026-12-28","09:00"))
                .put(legacy("Minggu depan","2027-01-04","08:00"))
                .put(legacy("Tahun baru","2027-01-01","11:30"));
        String answer=CalendarVoiceSummary.summarize(items,today);
        check(answer.contains("2 acara") && answer.contains("Senin lalu") && answer.contains("Tahun baru"),"week includes past and new year");
        check(!answer.contains("Minggu depan") && answer.contains("09.00"),"excludes later week and speaks time");
        JSONObject repeat=new JSONObject().put("id","r").put("title","Rutin").put("date","2026-12-21").put("time","10:00")
                .put("endDate","2026-12-21").put("endTime","11:00").put("type","Pribadi").put("repeat","week").put("interval",1).put("reminder",-1);
        check(CalendarVoiceSummary.summarize(new JSONArray().put(repeat),today).contains("Rutin"),"weekly recurrence");
        JSONArray many=new JSONArray(); for(int i=0;i<7;i++)many.put(legacy("Acara " + i,"2026-12-30","09:00"));
        String limited=CalendarVoiceSummary.summarize(many,today);
        check(limited.contains("7 acara") && limited.contains("2 acara lainnya") && !limited.contains("Acara 6"),"bounded reply");
        boolean failed=false; try { CalendarVoiceSummary.summarize(new JSONArray().put(new JSONObject()),today); } catch(Exception expected) { failed=true; }
        check(failed,"corrupt input must not report empty calendar");
        System.out.println("Calendar voice summary: whole week, recurrence, year rollover, limits and corrupt data OK");
    }
}
