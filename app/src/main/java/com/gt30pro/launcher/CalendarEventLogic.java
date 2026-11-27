package com.gt30pro.launcher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import org.json.JSONArray;
import org.json.JSONObject;

final class CalendarEventLogic {
    static final int MAX_REMINDER_MINUTES = 999 * 1440;
    static boolean validReminder(int minutes) { return minutes >= -1 && minutes <= MAX_REMINDER_MINUTES; }
    static String reminderLabel(int minutes) {
        if (minutes == 0) return "Saat mulai";
        if (minutes % 1440 == 0) return (minutes / 1440) + " hari sebelum";
        if (minutes % 60 == 0) return (minutes / 60) + " jam sebelum";
        return minutes + " menit sebelum";
    }
    static final class Occurrence {
        final JSONObject event; final LocalDate date;
        Occurrence(JSONObject e, LocalDate d) { event=e; date=d; }
    }
    static ArrayList<Occurrence> inRange(JSONArray events, LocalDate first, LocalDate last) {
        ArrayList<Occurrence> found=new ArrayList<>();
        for (int i=0;i<events.length();i++) {
            JSONObject e=events.optJSONObject(i); if (e==null) continue;
            try {
                LocalDate start=LocalDate.parse(e.getString("date"));
                LocalDate end=LocalDate.parse(e.optString("endDate",e.getString("date")));
                long duration=ChronoUnit.DAYS.between(start,end);
                if (duration<0 || duration>366) continue;
                if (duration > 0 && "00:00".equals(e.optString("endTime"))) duration--;
                String unit=e.optString("repeat","none"); int interval=e.optInt("interval",1);
                LocalDate current=CalendarRecurrence.next(start,first.minusDays(duration),unit,interval);
                while (current!=null && !current.isAfter(last)) {
                    if (!current.plusDays(duration).isBefore(first)) found.add(new Occurrence(e,current));
                    current=CalendarRecurrence.next(start,current.plusDays(1),unit,interval);
                }
            } catch (Exception ignored) { }
        }
        found.sort(Comparator.comparing((Occurrence o)->o.date).thenComparing(o->o.event.optString("time").isEmpty()?"99:99":o.event.optString("time")));
        return found;
    }
    static boolean validRange(String date, String time, String endDate, String endTime) {
        try {
            LocalDate start=LocalDate.parse(date), end=LocalDate.parse(endDate);
            if (!CalendarDates.inRange(start) || !CalendarDates.inRange(end) || !CalendarDates.validTime(time) || time.isEmpty() || !CalendarDates.validTime(endTime) || endTime.isEmpty()) return false;
            return !end.isBefore(start) && ChronoUnit.DAYS.between(start,end)<=366 && LocalDateTime.parse(date+"T"+time).isBefore(LocalDateTime.parse(endDate+"T"+endTime));
        } catch (Exception e) { return false; }
    }
    private CalendarEventLogic() {}
}
