package com.gt30pro.launcher;

import java.time.LocalDate;
import java.time.YearMonth;

public class CalendarDatesTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        check(CalendarDates.leadingDays(YearMonth.of(2026, 11)) == 6, "Sunday is last column");
        check(CalendarDates.leadingDays(YearMonth.of(2024, 1)) == 0, "Monday starts in first column");
        for (int i = 0; i < 7; i++) {
            YearMonth month = YearMonth.of(2024, 1).plusMonths(i);
            check(CalendarDates.leadingDays(month) == month.atDay(1).getDayOfWeek().getValue() - 1, "Monday-first alignment");
        }
        check(CalendarDates.shiftMonth(LocalDate.of(2024, 1, 31), 1).equals(LocalDate.of(2024, 2, 29)), "leap day");
        check(CalendarDates.shiftMonth(LocalDate.of(2025, 1, 31), 1).equals(LocalDate.of(2025, 2, 28)), "common February");
        check(CalendarDates.shiftMonth(LocalDate.of(2026, 12, 31), 1).equals(LocalDate.of(2027, 1, 31)), "year transition");
        check(CalendarDates.shiftMonth(LocalDate.of(1900, 1, 31), -1).equals(LocalDate.of(1900, 1, 31)), "minimum boundary");
        check(CalendarDates.shiftMonth(LocalDate.of(2100, 12, 31), 1).equals(LocalDate.of(2100, 12, 31)), "maximum boundary");
        check(CalendarDates.weekStart(LocalDate.of(2027, 1, 1)).equals(LocalDate.of(2026,12,28)), "cross-year Monday");
        check(CalendarDates.shiftWeek(LocalDate.of(2024, 2, 26), 1).equals(LocalDate.of(2024,3,4)), "leap week");
        check(CalendarDates.shiftWeek(LocalDate.of(1900,1,3), -1).equals(LocalDate.of(1900,1,1)), "week lower clamp");
        check(CalendarDates.shiftWeek(LocalDate.of(2100,12,29), 1).equals(LocalDate.of(2100,12,31)), "week upper clamp");
        check(!CalendarDates.inRange(LocalDate.of(2101,1,1)), "out-of-range date hidden");
        LocalDate selected = LocalDate.of(2027,1,1), today = LocalDate.of(2028,5,1);
        check(CalendarDates.inAgenda(LocalDate.of(2026,12,28), selected, today, false), "week starts across year");
        check(CalendarDates.inAgenda(LocalDate.of(2027,1,3), selected, today, false), "week ends Sunday");
        check(!CalendarDates.inAgenda(LocalDate.of(2027,1,4), selected, today, false), "next week excluded");
        check(CalendarDates.inAgenda(today, selected, today, true), "month uses actual today even outside viewed month");
        check(!CalendarDates.inAgenda(selected, selected, today, true), "month does not use selected date");
        check(CalendarDates.validTime("") && CalendarDates.validTime("00:00") && CalendarDates.validTime("23:59"), "valid optional time");
        check(!CalendarDates.validTime("24:00") && !CalendarDates.validTime("12:60") && !CalendarDates.validTime("9:30"), "invalid time");
        check(CalendarDates.validTitle(" Acara ") && !CalendarDates.validTitle("   ") && !CalendarDates.validTitle(null), "title validation");
        StringBuilder b = new StringBuilder(); for (int i=0;i<81;i++) b.append('x');
        check(!CalendarDates.validTitle(b.toString()), "title length");
        System.out.println("Calendar date, leap year, boundaries and input tests passed");
    }
}
