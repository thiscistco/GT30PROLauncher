package com.gt30pro.launcher;
public final class WeatherLocationTest {
    private static void check(String expected, String got) {
        if (!expected.equals(got)) throw new AssertionError("expected " + expected + " but got " + got);
    }
    public static void main(String[] args) {
        check("Semarang, Jawa Tengah", WeatherLocation.label("Semarang", "Kabupaten Semarang", "Jawa Tengah"));
        check("Kabupaten Semarang, Jawa Tengah", WeatherLocation.label(null, "Kabupaten Semarang", "Jawa Tengah"));
        check("Jawa Tengah", WeatherLocation.label(null, null, " Jawa Tengah "));
        check("", WeatherLocation.label(null, "  ", null));
        check("Semarang", WeatherLocation.label("Semarang", null, "semarang"));
        System.out.println("Weather location: coarse region label and fallback: OK");
    }
}
