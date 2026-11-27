package com.gt30pro.launcher;

import java.util.Calendar;
import java.util.TimeZone;

/** Perhitungan waktu alarm berdasarkan zona waktu lokal dan hari pilihan. */
final class ClockTime {
    // Bit 0 = Minggu ... bit 6 = Sabtu; 0 = sekali.
    static long nextAlarm(int hour, int minute, int days, long now, TimeZone zone) {
        Calendar current = Calendar.getInstance(zone); current.setTimeInMillis(now);
        for (int offset = 0; offset <= 7; offset++) {
            Calendar candidate = (Calendar) current.clone();
            candidate.add(Calendar.DAY_OF_YEAR, offset);
            candidate.set(Calendar.HOUR_OF_DAY, hour);
            candidate.set(Calendar.MINUTE, minute);
            candidate.set(Calendar.SECOND, 0); candidate.set(Calendar.MILLISECOND, 0);
            int dayBit = 1 << (candidate.get(Calendar.DAY_OF_WEEK) - 1);
            if (candidate.getTimeInMillis() > now && (days == 0 || (days & dayBit) != 0)) return candidate.getTimeInMillis();
        }
        return 0;
    }
    static String countdown(long when, long now) {
        long seconds = Math.max(0L, (when - now + 999L) / 1000L);
        long days = seconds / 86400L;
        String time = String.format(java.util.Locale.US, "%02d:%02d:%02d", (seconds / 3600L) % 24L, (seconds / 60L) % 60L, seconds % 60L);
        return days > 0 ? "Dalam " + days + " hari " + time : "Dalam " + time;
    }
    private ClockTime() {}
}
