package com.gt30pro.launcher;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Penyimpanan alarm dan penjadwalan yang tetap ada saat aplikasi ditutup. */
final class ClockAlarms {
    static final String PREF = "clock_data";
    static final String FIRE = "com.gt30pro.launcher.ALARM_FIRE";
    static final String SNOOZE_FIRE = "com.gt30pro.launcher.ALARM_SNOOZE_FIRE";
    static final String TIMER_FIRE = "com.gt30pro.launcher.TIMER_FIRE";
    static final String SNOOZE = "com.gt30pro.launcher.ALARM_SNOOZE";
    static final String DISMISS = "com.gt30pro.launcher.ALARM_DISMISS";
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
    static JSONArray read(Context c) {
        try { return new JSONArray(prefs(c).getString("alarms", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
    static void write(Context c, JSONArray items) { prefs(c).edit().putString("alarms", items.toString()).apply(); }
    static JSONObject find(Context c, int id) {
        JSONArray all = read(c);
        for (int i = 0; i < all.length(); i++) {
            JSONObject a = all.optJSONObject(i);
            if (a != null && a.optInt("id") == id) return a;
        }
        return null;
    }
    static void put(Context c, JSONObject alarm) {
        JSONArray all = read(c), updated = new JSONArray();
        boolean found = false;
        for (int i = 0; i < all.length(); i++) {
            JSONObject old = all.optJSONObject(i);
            if (old == null) continue;
            if (old.optInt("id") == alarm.optInt("id")) { updated.put(alarm); found = true; }
            else updated.put(old);
        }
        if (!found) updated.put(alarm);
        write(c, updated);
    }
    static void remove(Context c, int id) {
        cancel(c, id);
        JSONArray all = read(c), next = new JSONArray();
        for (int i = 0; i < all.length(); i++) {
            JSONObject a = all.optJSONObject(i);
            if (a != null && a.optInt("id") != id) next.put(a);
        }
        write(c, next);
    }
    static int nextId(Context c) {
        SharedPreferences p = prefs(c);
        int id = p.getInt("next_id", 1);
        p.edit().putInt("next_id", id + 1).apply();
        return id;
    }
    static long nextTime(JSONObject a, long now) {
        return ClockTime.nextAlarm(a.optInt("hour", 7), a.optInt("minute", 0), a.optInt("days", 0), now, java.util.TimeZone.getDefault());
    }
    static boolean canSchedule(Context c) {
        AlarmManager m = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return Build.VERSION.SDK_INT < 31 || m.canScheduleExactAlarms();
    }
    static PendingIntent broadcast(Context c, String action, int id) {
        Intent i = new Intent(c, ClockAlertReceiver.class).setAction(action).putExtra("id", id);
        i.setData(android.net.Uri.parse("gt30clock://" + action + "/" + id));
        return PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    static boolean schedule(Context c, JSONObject alarm) {
        if (!alarm.optBoolean("enabled", false) || !canSchedule(c)) return false;
        long when = nextTime(alarm, System.currentTimeMillis());
        if (when == 0) return false;
        Intent show = new Intent(c, ClockActivity.class);
        PendingIntent showPi = PendingIntent.getActivity(c, 0, show, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).setAlarmClock(
                    new AlarmManager.AlarmClockInfo(when, showPi), broadcast(c, FIRE, alarm.optInt("id")));
            return true;
        } catch (SecurityException e) { return false; }
    }
    static boolean scheduleAt(Context c, String action, int id, long when) {
        if (!canSchedule(c)) return false;
        try {
            ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, when, broadcast(c, action, id));
            return true;
        } catch (SecurityException e) { return false; }
    }
    static void cancel(Context c, int id) {
        AlarmManager m = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        m.cancel(broadcast(c, FIRE, id)); m.cancel(broadcast(c, SNOOZE_FIRE, id));
        prefs(c).edit().remove("snooze_" + id).apply();
    }
    static void cancelTimer(Context c) {
        ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(broadcast(c, TIMER_FIRE, 0));
        prefs(c).edit().remove("timer_end").apply();
    }
    static void restore(Context c) {
        if (!canSchedule(c)) return;
        JSONArray alarms = read(c); long now = System.currentTimeMillis();
        for (int i = 0; i < alarms.length(); i++) {
            JSONObject alarm = alarms.optJSONObject(i); if (alarm == null) continue;
            schedule(c, alarm);
            long snooze = prefs(c).getLong("snooze_" + alarm.optInt("id"), 0);
            if (snooze > 0) scheduleAt(c, SNOOZE_FIRE, alarm.optInt("id"), Math.max(now + 1000, snooze));
        }
        long end = prefs(c).getLong("timer_end", 0);
        if (end > 0) scheduleAt(c, TIMER_FIRE, 0, Math.max(now + 1000, end));
    }
    private ClockAlarms() {}
}
