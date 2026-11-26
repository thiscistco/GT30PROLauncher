package com.gt30pro.launcher;

import java.time.LocalDate;
import java.time.YearMonth;

public class CalendarDatesTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(CalendarDates.leadingDays(YearMonth.of(2026, 11)) == 0, "Sunday starts in first column");
        check(CalendarDates.leadingDays(YearMonth.of(2024, 1)) == 1, "Monday starts in second column");
        check(CalendarDates.shiftMonth(LocalDate.of(2024, 1, 31), 1).equals(LocalDate.of(2024, 2, 29)), "leap day");
        check(CalendarDates.shiftMonth(LocalDate.of(2025, 1, 31), 1).equals(LocalDate.of(2025, 2, 28)), "common February");
        check(CalendarDates.shiftMonth(LocalDate.of(2026, 12, 31), 1).equals(LocalDate.of(2027, 1, 31)), "year transition");
        check(CalendarDates.shiftMonth(LocalDate.of(1900, 1, 31), -1).equals(LocalDate.of(1900, 1, 31)), "minimum boundary");
        check(CalendarDates.shiftMonth(LocalDate.of(2100, 12, 31), 1).equals(LocalDate.of(2100, 12, 31)), "maximum boundary");
        check(CalendarDates.validTime("") && CalendarDates.validTime("00:00") && CalendarDates.validTime("23:59"), "valid optional time");
        check(!CalendarDates.validTime("24:00") && !CalendarDates.validTime("12:60") && !CalendarDates.validTime("9:30"), "invalid time");
        check(CalendarDates.validTitle(" Acara ") && !CalendarDates.validTitle("   ") && !CalendarDates.validTitle(null), "title validation");
        StringBuilder b = new StringBuilder(); for (int i=0;i<81;i++) b.append('x');
        check(!CalendarDates.validTitle(b.toString()), "title length");
        System.out.println("Calendar date, leap year, boundaries and input tests passed");
    }
}
