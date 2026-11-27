package com.gt30pro.launcher;

import java.time.LocalDate;
import java.time.YearMonth;

/** Shared date and input rules for the local calendar. */
final class CalendarDates {
    private static final YearMonth MIN = YearMonth.of(1900, 1);
    private static final YearMonth MAX = YearMonth.of(2100, 12);
    private CalendarDates() {}

    /** Calendar rows start on Monday, matching the Indonesian weekday labels. */
    static int leadingDays(YearMonth month) {
        return month.atDay(1).getDayOfWeek().getValue() - 1;
    }

    /** Navigate by one month, preserving the selected day where it exists. */
    static LocalDate shiftMonth(LocalDate current, int months) {
        YearMonth target = YearMonth.from(current).plusMonths(months);
        if (target.isBefore(MIN)) target = MIN;
        if (target.isAfter(MAX)) target = MAX;
        return target.atDay(Math.min(current.getDayOfMonth(), target.lengthOfMonth()));
    }

    static LocalDate weekStart(LocalDate date) { return date.minusDays(date.getDayOfWeek().getValue() - 1); }
    static boolean inRange(LocalDate date) { return !date.isBefore(MIN.atDay(1)) && !date.isAfter(MAX.atEndOfMonth()); }
    static LocalDate shiftWeek(LocalDate current, int weeks) {
        LocalDate target = current.plusWeeks(weeks);
        if (target.isBefore(MIN.atDay(1))) return MIN.atDay(1);
        if (target.isAfter(MAX.atEndOfMonth())) return MAX.atEndOfMonth();
        return target;
    }

    static boolean validTime(String time) {
        if (time == null) return false;
        String value = time.trim();
        if (value.isEmpty()) return true;
        if (!value.matches("[0-9]{2}:[0-9]{2}")) return false;
        return Integer.parseInt(value.substring(0, 2)) < 24 && Integer.parseInt(value.substring(3, 5)) < 60;
    }

    static boolean validTitle(String title) {
        return title != null && !title.trim().isEmpty() && title.trim().length() <= 80;
    }
}
