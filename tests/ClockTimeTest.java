package com.gt30pro.launcher;

import java.util.Calendar;
import java.util.TimeZone;

/** Run with tests/test-clock-time.sh. */
public class ClockTimeTest {
    private static long time(TimeZone z, int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(z); c.clear(); c.set(year, month - 1, day, hour, minute, 0); return c.getTimeInMillis();
    }
    private static void check(String name, long actual, long expected) {
        if (actual != expected) throw new AssertionError(name + ": " + actual + " != " + expected);
    }
    public static void main(String[] args) {
        TimeZone jakarta = TimeZone.getTimeZone("Asia/Jakarta");
        long monday = time(jakarta, 2026, 11, 23, 6, 30);
        check("once today", ClockTime.nextAlarm(7,0,0,monday,jakarta),time(jakarta,2026,11,23,7,0));
        check("once tomorrow", ClockTime.nextAlarm(6,0,0,monday,jakarta),time(jakarta,2026,11,24,6,0));
        check("daily", ClockTime.nextAlarm(7,0,127,monday,jakarta),time(jakarta,2026,11,23,7,0));
        check("selected Wednesday", ClockTime.nextAlarm(7,0,1<<3,monday,jakarta),time(jakarta,2026,11,25,7,0));
        check("next selected Monday after deadline", ClockTime.nextAlarm(6,0,1<<1,monday,jakarta),time(jakarta,2026,11,30,6,0));
        TimeZone london = TimeZone.getTimeZone("Europe/London");
        check("DST transition",ClockTime.nextAlarm(9,0,127,time(london,2026,3,28,10,0),london),time(london,2026,3,29,9,0));
        System.out.println("ClockTime tests passed");
    }
}
