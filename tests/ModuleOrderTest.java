package com.gt30pro.launcher;
import java.util.Arrays;
public class ModuleOrderTest {
    public static void main(String[] args) {
        if(!ModuleOrder.parse(null).equals(ModuleOrder.DEFAULT)) throw new AssertionError("defaults");
        if(!ModuleOrder.parse("weather,clock,weather,unknown").equals(Arrays.asList("weather","clock","calendar","timetable","settings"))) throw new AssertionError("dedupe/unknown");
        if(!ModuleOrder.parse("settings,timetable,weather,calendar,clock").equals(Arrays.asList("settings","timetable","weather","calendar","clock"))) throw new AssertionError("persisted");
        System.out.println("Module order: defaults, persistence, deduplication and new modules OK");
    }
}
