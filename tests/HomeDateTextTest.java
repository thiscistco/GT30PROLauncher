package com.gt30pro.launcher;

import java.time.LocalDate;

public class HomeDateTextTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        LocalDate monday = LocalDate.of(2026, 11, 23);
        String[] days = {"SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN"};
        for (int i = 0; i < days.length; i++) check(HomeDateText.weekday(monday.plusDays(i)).equals(days[i]), "weekday " + i);
        String[] months = {"JAN", "FEB", "MAR", "APR", "MEI", "JUN", "JUL", "AGU", "SEP", "OKT", "NOV", "DES"};
        for (int i = 0; i < months.length; i++) check(HomeDateText.month(LocalDate.of(2026, i + 1, 1)).equals(months[i]), "month " + i);
        check(HomeDateText.day(LocalDate.of(2026, 1, 1)).equals("01"), "leading zero");
        check(HomeDateText.day(LocalDate.of(2024, 2, 29)).equals("29"), "leap day");
        check(HomeDateText.description(LocalDate.of(2026, 11, 24)).equals("Selasa, 24 November 2026"), "accessible full date");
        LocalDate yearEnd = LocalDate.of(2026, 12, 31);
        check(HomeDateText.weekday(yearEnd).equals("KAM") && HomeDateText.month(yearEnd.plusDays(1)).equals("JAN")
                && HomeDateText.day(yearEnd.plusDays(1)).equals("01"), "new year rollover");
        System.out.println("Home date: Indonesian abbreviations, leap day and rollover: OK");
    }
}
