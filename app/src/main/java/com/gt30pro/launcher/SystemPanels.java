package com.gt30pro.launcher;

import android.content.Context;
import android.widget.Toast;

/** Android has no public panel-expansion API; OEMs or future releases may block this. */
final class SystemPanels {
    static void open(Context context, boolean controls) {
        try {
            Object service = context.getSystemService("statusbar");
            if (service == null) throw new IllegalStateException("status bar service unavailable");
            String method = controls ? "expandSettingsPanel" : "expandNotificationsPanel";
            service.getClass().getMethod(method).invoke(service);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Toast.makeText(context, "Panel tidak dapat dibuka dari launcher. Geser dari tepi atas layar.", Toast.LENGTH_LONG).show();
        }
    }
    private SystemPanels() { }
}
