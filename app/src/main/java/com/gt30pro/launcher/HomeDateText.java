package com.gt30pro.launcher;

import java.time.LocalDate;

/** Fixed Indonesian abbreviations, independent of the system language. */
final class HomeDateText {
    private static final String[] DAYS = {"SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN"};
    private static final String[] DAYS_FULL = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
    private static final String[] MONTHS = {"JAN", "FEB", "MAR", "APR", "MEI", "JUN", "JUL", "AGU", "SEP", "OKT", "NOV", "DES"};
    private static final String[] MONTHS_FULL = {"Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
    private HomeDateText() { }
    static String weekday(LocalDate date) { return DAYS[date.getDayOfWeek().getValue() - 1]; }
    static String day(LocalDate date) { int n = date.getDayOfMonth(); return n < 10 ? "0" + n : Integer.toString(n); }
    static String month(LocalDate date) { return MONTHS[date.getMonthValue() - 1]; }
    static String description(LocalDate date) {
        return DAYS_FULL[date.getDayOfWeek().getValue() - 1] + ", " + date.getDayOfMonth() + " " + MONTHS_FULL[date.getMonthValue() - 1] + " " + date.getYear();
    }
}
