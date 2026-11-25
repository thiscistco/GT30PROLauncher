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
    private ClockTime() {}
}
