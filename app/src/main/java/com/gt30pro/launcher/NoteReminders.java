package com.gt30pro.launcher;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import java.util.Map;

/** Local one-shot reminders; notification content intentionally hides note text. */
final class NoteReminders {
    static final String ACTION="com.gt30pro.launcher.NOTE_REMINDER", PREFS="note_reminders";
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE); }
    static PendingIntent intent(Context c,long id) {
        Intent i=new Intent(c,NoteReminderReceiver.class).setAction(ACTION).setData(Uri.parse("gt30note://reminder/"+id)).putExtra("id",id);
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static boolean exact(Context c) { return Build.VERSION.SDK_INT<31 || ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms(); }
    static boolean schedule(Context c,long id,long when) {
        try {
            AlarmManager alarm=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            if(exact(c)) {
                try { alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,intent(c,id)); }
                catch(SecurityException ex) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,intent(c,id)); }
            } else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,intent(c,id));
            return true;
        } catch(RuntimeException ex) { return false; }
    }
    static boolean set(Context c,long id,long when) {
        if(id<=0 || when<=System.currentTimeMillis()) return false;
        SharedPreferences p=prefs(c); String key=Long.toString(id); long old=p.getLong(key,0);
        if(!p.edit().putLong(key,when).commit()) return false;
        if(schedule(c,id,when)) return true;
        p.edit().putLong(key,old).commit(); if(old>0) schedule(c,id,old); return false;
    }
    static void cancel(Context c,long id) {
        ((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(intent(c,id));
        ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).cancel("note-"+id,0);
        prefs(c).edit().remove(Long.toString(id)).commit();
    }
    static void restore(Context c) {
        for(Map.Entry<String,?> e:prefs(c).getAll().entrySet()) try {
            long id=Long.parseLong(e.getKey()); if(id>0 && e.getValue() instanceof Long && ((Long)e.getValue())>0) schedule(c,id,(Long)e.getValue());
        } catch(RuntimeException ignored) { }
    }
    static void deliver(Context c,long id) {
        String key=Long.toString(id); SharedPreferences p=prefs(c); long when=p.getLong(key,0);
        if(id<=0 || when<=0) return;
        // Ignore an obsolete delivery after a reminder was rescheduled.
        if(when>System.currentTimeMillis()+1000) { schedule(c,id,when); return; }
        try(NoteStore db=new NoteStore(c)) {
            boolean found=false; for(NoteStore.Note n:db.list()) if(n.id==id && n.state!=NoteStore.TRASH) { found=true; break; }
            if(!found) { cancel(c,id); return; }
        } catch(RuntimeException e) { schedule(c,id,System.currentTimeMillis()+60000); return; }
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel("note_reminders","Pengingat catatan",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent tap=PendingIntent.getActivity(c,0,new Intent(c,NotesActivity.class).setData(Uri.parse("gt30note://open/"+id)).putExtra("note_id",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification n=new Notification.Builder(c,"note_reminders").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Pengingat catatan")
                .setContentText("Buka Catatan untuk melihat pengingat.").setContentIntent(tap).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER).build();
        try { manager.notify("note-"+id,0,n); p.edit().remove(key).commit(); } catch(SecurityException ignored) { }
    }
    private NoteReminders() {}
}
