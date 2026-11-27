package com.gt30pro.launcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Device-only reminders; one pending alarm per event, keyed by its UUID. */
final class CalendarReminders {
    static final String ACTION = "com.gt30pro.launcher.CALENDAR_REMINDER";
    static JSONArray read(Context c) {
        try { return new JSONArray(c.getSharedPreferences("launcher_calendar", Context.MODE_PRIVATE).getString("events_v1", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
    static JSONObject find(Context c, String id) {
        JSONArray all = read(c);
        for (int i=0;i<all.length();i++) { JSONObject e=all.optJSONObject(i); if (e!=null && id.equals(e.optString("id"))) return e; }
        return null;
    }
    static PendingIntent intent(Context c, String id) { return intent(c,id,0); }
    static PendingIntent intent(Context c, String id, long start) {
        Intent i = new Intent(c, CalendarReminderReceiver.class).setAction(ACTION).setData(Uri.parse("gt30calendar://reminder/" + Uri.encode(id))).putExtra("id", id).putExtra("start",start);
        return PendingIntent.getBroadcast(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static void cancel(Context c, String id) {
        ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(intent(c,id));
        ((android.app.NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id, 0);
    }
    static long nextReminder(JSONObject e, long now) {
        int minutes=e.optInt("reminder", -1);
        String time=e.optString("time");
        if (minutes < 0 || !CalendarEventLogic.validReminder(minutes) || time.isEmpty()) return 0;
        try {
            ZoneId zone=ZoneId.systemDefault();
            LocalDate start=LocalDate.parse(e.getString("date"));
            LocalDate from=java.time.Instant.ofEpochMilli(now + minutes*60000L).atZone(zone).toLocalDate();
            String repeat=e.optString("repeat", "none"); int interval=e.optInt("interval", 1);
            LocalDate occurrence=CalendarRecurrence.next(start, from, repeat, interval);
            for (int i=0; i<2 && occurrence!=null; i++) {
                long when=LocalDateTime.parse(occurrence.toString()+"T"+time).atZone(zone).toInstant().toEpochMilli() - minutes*60000L;
                if (when > now) return when;
                occurrence=CalendarRecurrence.next(start, occurrence.plusDays(1), repeat, interval);
            }
        } catch (Exception ignored) { }
        return 0;
    }
    static boolean exact(Context c) { return Build.VERSION.SDK_INT < 31 || ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms(); }
    static void schedule(Context c, JSONObject event) {
        String id=event.optString("id"); if (id.isEmpty()) return;
        cancelAlarm(c,id);
        long when=nextReminder(event,System.currentTimeMillis()); if (when == 0) return;
        AlarmManager m=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        long start=when + event.optInt("reminder")*60000L;
        PendingIntent alarm=intent(c,id,start);
        try {
            if (exact(c)) m.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,alarm);
            else m.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,alarm);
        } catch (SecurityException ex) { m.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,alarm); }
    }
    static void cancelAlarm(Context c,String id) { ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(intent(c,id)); }
    static void restore(Context c) {
        JSONArray all=read(c); for (int i=0;i<all.length();i++) { JSONObject e=all.optJSONObject(i); if (e!=null) schedule(c,e); }
    }
    private CalendarReminders() {}
}
