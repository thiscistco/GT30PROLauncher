package com.gt30pro.launcher;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public final class NoteReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(NoteReminders.ACTION.equals(intent.getAction())) NoteReminders.deliver(context,intent.getLongExtra("id",0));
    }
}
