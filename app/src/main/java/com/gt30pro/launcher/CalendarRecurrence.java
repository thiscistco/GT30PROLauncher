package com.gt30pro.launcher;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/** Recurrence anchored to the original start, not to the previous clamped occurrence. */
final class CalendarRecurrence {
    private CalendarRecurrence() {}
    static LocalDate next(LocalDate start, LocalDate from, String unit, int interval) {
        if (from.isBefore(start)) from = start;
        if ("none".equals(unit)) return from.equals(start) ? start : null;
        if (interval < 1 || interval > 999) return null;
        long delta;
        switch (unit) {
            case "day": delta = ChronoUnit.DAYS.between(start, from); break;
            case "week": delta = ChronoUnit.DAYS.between(start, from) / 7; break;
            case "month": delta = ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(from)); break;
            case "year": delta = from.getYear() - start.getYear(); break;
            default: return null;
        }
        long n = Math.max(0, delta / interval);
        for (int i=0; i<2; i++, n++) {
            long shift = n * interval;
            LocalDate candidate;
            switch (unit) {
                case "day": candidate = start.plusDays(shift); break;
                case "week": candidate = start.plusWeeks(shift); break;
                case "month": candidate = start.plusMonths(shift); break;
                default: candidate = start.plusYears(shift); break;
            }
            if (candidate.getYear() > 2100) return null;
            if (!candidate.isBefore(from)) return candidate;
        }
        return null;
    }
}
