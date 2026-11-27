package com.gt30pro.launcher;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Read-only answer using the launcher's own events, not screenshots or cloud AI. */
final class CalendarVoiceSummary {
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("EEEE, d MMMM",new Locale("id","ID"));
    static String summarize(JSONArray events,LocalDate today) throws Exception {
        // Reject malformed storage rather than reporting an incorrect empty calendar.
        for (int i=0;i<events.length();i++) {
            JSONObject e=events.getJSONObject(i);
            String id=e.getString("id"), title=e.getString("title"), date=e.getString("date"), time=e.getString("time");
            LocalDate d=LocalDate.parse(date);
            if(id.isEmpty() || d.getYear()<1900 || d.getYear()>2100 || !CalendarDates.validTitle(title) || !CalendarDates.validTime(time)) throw new IllegalArgumentException();
            if(e.has("endDate")) {
                String type=e.getString("type"), repeat=e.getString("repeat"); int interval=e.getInt("interval"), reminder=e.getInt("reminder");
                if(!CalendarEventLogic.validRange(date,time,e.getString("endDate"),e.getString("endTime")) ||
                    !(type.equals("Pribadi") || type.equals("Pekerjaan") || type.equals("Keluarga") || type.equals("Lainnya")) ||
                    !(repeat.equals("none") || repeat.equals("day") || repeat.equals("week") || repeat.equals("month") || repeat.equals("year")) ||
                    interval<1 || interval>999 || !CalendarEventLogic.validReminder(reminder)) throw new IllegalArgumentException();
            }
        }
        LocalDate monday=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday=monday.plusDays(6);
        ArrayList<CalendarEventLogic.Occurrence> found=CalendarEventLogic.inRange(events,monday,sunday);
        if(found.isEmpty()) return "Tidak ada acara kalender minggu ini, dari Senin sampai Minggu.";
        StringBuilder reply=new StringBuilder("Minggu ini ada ").append(found.size()).append(" acara. ");
        int limit=Math.min(5,found.size());
        for(int i=0;i<limit;i++) {
            CalendarEventLogic.Occurrence item=found.get(i);
            if(item.date.isBefore(monday)) reply.append("Dimulai ");
            reply.append(DATE.format(item.date));
            String time=item.event.optString("time","");
            if(!time.isEmpty()) reply.append(" pukul ").append(time.replace(':','.'));
            reply.append(", ").append(item.event.optString("title")).append(". ");
        }
        if(found.size()>limit) reply.append("Dan ").append(found.size()-limit).append(" acara lainnya. Buka kalender untuk melihat semua.");
        return reply.toString().trim();
    }
    private CalendarVoiceSummary() { }
}
