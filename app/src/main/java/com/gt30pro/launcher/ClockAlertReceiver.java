package com.gt30pro.launcher;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;

/** Notifikasi alarm/timer; suara dan getar dikelola kanal notifikasi Android. */
public class ClockAlertReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        int id = intent.getIntExtra("id", 0);
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = id == 0 ? 2000000 : 1000000 + id;
        if (ClockAlarms.DISMISS.equals(action)) { nm.cancel(notificationId); return; }
        if (ClockAlarms.SNOOZE.equals(action)) {
            if (ClockAlarms.find(c, id) == null) { nm.cancel(notificationId); return; }
            long until = System.currentTimeMillis() + 10 * 60 * 1000L;
            if (ClockAlarms.scheduleAt(c, ClockAlarms.SNOOZE_FIRE, id, until)) {
                ClockAlarms.prefs(c).edit().putLong("snooze_" + id, until).apply();
                nm.cancel(notificationId);
                Toast.makeText(c, "Ditunda 10 menit", Toast.LENGTH_SHORT).show();
            } else Toast.makeText(c, "Izinkan alarm tepat waktu untuk menunda", Toast.LENGTH_LONG).show();
            return;
        }
        boolean timer = ClockAlarms.TIMER_FIRE.equals(action);
        boolean snoozed = ClockAlarms.SNOOZE_FIRE.equals(action);
        if (!timer && !snoozed && !ClockAlarms.FIRE.equals(action)) return;
        JSONObject alarm = timer ? null : ClockAlarms.find(c, id);
        if (timer) {
            long end = ClockAlarms.prefs(c).getLong("timer_end", 0);
            if (end == 0 || end > System.currentTimeMillis() + 1000) return;
            ClockAlarms.prefs(c).edit().remove("timer_end").apply();
        } else {
            if (alarm == null) return;
            if (snoozed) ClockAlarms.prefs(c).edit().remove("snooze_" + id).apply();
            else {
                if (!alarm.optBoolean("enabled", false)) return;
                if (alarm.optInt("days", 0) == 0) {
                    try { alarm.put("enabled", false); ClockAlarms.put(c, alarm); } catch (JSONException ignored) { }
                } else ClockAlarms.schedule(c, alarm);
            }
        }
        String sound = timer ? Settings.System.DEFAULT_ALARM_ALERT_URI.toString() : alarm.optString("sound", Settings.System.DEFAULT_ALARM_ALERT_URI.toString());
        boolean vibrate = timer || alarm.optBoolean("vibrate", true);
        Uri uri = sound.isEmpty() ? null : Uri.parse(sound);
        String channelId = "clock_" + Integer.toHexString(sound.hashCode()) + "_" + (vibrate ? "v" : "n");
        NotificationChannel channel = new NotificationChannel(channelId, "Alarm & timer", NotificationManager.IMPORTANCE_HIGH);
        channel.setSound(uri, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        channel.enableVibration(vibrate);
        if (vibrate) channel.setVibrationPattern(new long[]{0, 500, 500});
        nm.createNotificationChannel(channel);
        PendingIntent open = PendingIntent.getActivity(c, 0, new Intent(c, ClockActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(c, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(timer ? "Timer selesai" : "Alarm")
                .setContentText(timer ? "Waktu yang ditentukan telah habis." : "Waktunya bangun atau mengingatkan Anda.")
                .setCategory(Notification.CATEGORY_ALARM).setPriority(Notification.PRIORITY_MAX)
                .setContentIntent(open).setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Hentikan", ClockAlarms.broadcast(c, ClockAlarms.DISMISS, id)).build());
        if (!timer) builder.addAction(new Notification.Action.Builder(null, "Tunda 10 menit", ClockAlarms.broadcast(c, ClockAlarms.SNOOZE, id)).build());
        Notification n = builder.build();
        n.flags |= Notification.FLAG_INSISTENT;
        try { nm.notify(notificationId, n); } catch (SecurityException ignored) { }
    }
}
