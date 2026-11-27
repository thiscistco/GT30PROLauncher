package com.gt30pro.launcher;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;
public class CalendarEventLogicTest {
    static void ok(boolean b,String message) { if(!b) throw new AssertionError(message); }
    static LocalDate d(String s) { return LocalDate.parse(s); }
    static JSONObject event() throws Exception { return new JSONObject().put("id","x").put("title","Test").put("date","2026-12-31").put("time","09:00").put("endDate","2027-01-01").put("endTime","10:00").put("repeat","week").put("interval",1).put("reminder",60); }
    public static void main(String[] args) throws Exception {
        JSONObject e=event(); JSONArray all=new JSONArray().put(e);
        ok(CalendarEventLogic.validRange("2026-12-31","09:00","2027-01-01","10:00"),"valid range");
        ok(!CalendarEventLogic.validRange("2026-12-31","09:00","2026-12-31","09:00"),"same instant invalid");
        ok(!CalendarEventLogic.validRange("2026-01-01","09:00","2028-01-01","10:00"),"duration limited");
        java.util.ArrayList<CalendarEventLogic.Occurrence> occurrences=CalendarEventLogic.inRange(all,d("2027-01-01"),d("2027-01-08"));
        ok(occurrences.size()==2 && occurrences.get(0).date.equals(d("2026-12-31")) && occurrences.get(1).date.equals(d("2027-01-07")),"multiday and recurrence across year");
        JSONObject midnight=event().put("repeat","none").put("endTime","00:00");
        ok(CalendarEventLogic.inRange(new JSONArray().put(midnight),d("2027-01-01"),d("2027-01-01")).isEmpty(),"end is exclusive at midnight");
        JSONObject legacy=new JSONObject().put("id","old").put("title","Old").put("date","2027-01-01").put("time","");
        ok(CalendarEventLogic.inRange(new JSONArray().put(legacy),d("2027-01-01"),d("2027-01-01")).size()==1,"legacy events survive");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        long now=ZonedDateTime.parse("2026-12-31T07:00:00Z[UTC]").toInstant().toEpochMilli();
        long expected=ZonedDateTime.parse("2026-12-31T08:00:00Z[UTC]").toInstant().toEpochMilli();
        ok(CalendarReminders.nextReminder(e,now)==expected,"one hour before start");
        long after=ZonedDateTime.parse("2026-12-31T08:01:00Z[UTC]").toInstant().toEpochMilli();
        long next=ZonedDateTime.parse("2027-01-07T08:00:00Z[UTC]").toInstant().toEpochMilli();
        ok(CalendarReminders.nextReminder(e,after)==next,"past reminder skipped, next week");
        e.put("reminder",1440); long prev=ZonedDateTime.parse("2026-12-30T08:30:00Z[UTC]").toInstant().toEpochMilli();
        ok(CalendarReminders.nextReminder(e,prev)==ZonedDateTime.parse("2026-12-30T09:00:00Z[UTC]").toInstant().toEpochMilli(),"previous-day reminder");
        e.put("repeat","none");ok(CalendarReminders.nextReminder(e,after)==0,"past one-off not replayed");
        ok(CalendarEventLogic.validReminder(999*1440) && !CalendarEventLogic.validReminder(999*1440+1) && !CalendarEventLogic.validReminder(-2),"custom reminder range");
        ok(CalendarEventLogic.reminderLabel(17).equals("17 menit sebelum") && CalendarEventLogic.reminderLabel(180).equals("3 jam sebelum") && CalendarEventLogic.reminderLabel(14400).equals("10 hari sebelum"),"custom reminder labels");
        e.put("repeat","week").put("reminder",14400);
        long farNow=ZonedDateTime.parse("2026-12-01T00:00:00Z[UTC]").toInstant().toEpochMilli();
        long farExpected=ZonedDateTime.parse("2026-12-21T09:00:00Z[UTC]").toInstant().toEpochMilli();
        ok(CalendarReminders.nextReminder(e,farNow)==farExpected,"custom ten-day reminder");
        e.put("reminder",-1);ok(CalendarReminders.nextReminder(e,now)==0,"disabled reminder");
        System.out.println("Android calendar occurrences, range, legacy and reminder timing: OK");
    }
}
