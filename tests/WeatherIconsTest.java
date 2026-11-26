package com.gt30pro.launcher;
public class WeatherIconsTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    public static void main(String[] args) {
        check(WeatherIcons.forCode(-1,true) == WeatherIcons.UNKNOWN);
        check(WeatherIcons.forCode(0,true) == WeatherIcons.SUN);
        check(WeatherIcons.forCode(0,false) == WeatherIcons.MOON);
        check(WeatherIcons.forCode(3,true) == WeatherIcons.CLOUD);
        check(WeatherIcons.forCode(45,true) == WeatherIcons.FOG);
        check(WeatherIcons.forCode(61,true) == WeatherIcons.RAIN);
        check(WeatherIcons.forCode(71,true) == WeatherIcons.SNOW);
        check(WeatherIcons.forCode(95,true) == WeatherIcons.STORM);
        for (String[] rows : new String[][]{WeatherIcons.UNKNOWN, WeatherIcons.SUN, WeatherIcons.CLOUD, WeatherIcons.RAIN, WeatherIcons.SNOW, WeatherIcons.STORM, WeatherIcons.FOG, WeatherIcons.MOON}) {
            check(rows.length == 13); for (String row : rows) check(row.matches("[.#]{13}"));
        }
        System.out.println("Android weather icons and WMO mapping: OK");
    }
}
