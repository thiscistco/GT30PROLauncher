package com.gt30pro.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

public class CalendarReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        if (!CalendarReminders.ACTION.equals(intent.getAction())) return;
        String id=intent.getStringExtra("id"); if (id == null) return;
        JSONObject event=CalendarReminders.find(c,id); if (event == null || event.optInt("reminder",-1)<0) return;
        CalendarReminders.schedule(c,event);
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        String channel="calendar_events";
        manager.createNotificationChannel(new NotificationChannel(channel,"Pengingat kalender",NotificationManager.IMPORTANCE_DEFAULT));
        Intent open=new Intent(c,CalendarActivity.class);
        PendingIntent tap=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(c,channel).setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(event.optString("title")).setContentText("Acara akan dimulai")
                .setContentIntent(tap).setAutoCancel(true).setCategory(Notification.CATEGORY_EVENT).build();
        try { manager.notify(id,0,notification); } catch (SecurityException ignored) { }
    }
}
