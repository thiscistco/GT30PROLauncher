package com.gt30pro.launcher;
import java.util.Arrays;
public class ModuleOrderTest {
    public static void main(String[] args) {
        if(!ModuleOrder.parse(null).equals(ModuleOrder.DEFAULT)) throw new AssertionError("defaults");
        if(!ModuleOrder.parse("weather,clock,weather,unknown").equals(Arrays.asList("weather","clock","calendar","timetable","settings","notes"))) throw new AssertionError("dedupe/unknown");
        if(!ModuleOrder.parse("settings,timetable,weather,calendar,clock").equals(Arrays.asList("settings","timetable","weather","calendar","clock","notes"))) throw new AssertionError("persisted");
        if(!ModuleOrder.parse("notes,clock,notes").equals(Arrays.asList("notes","clock","calendar","weather","timetable","settings"))) throw new AssertionError("notes reorder");
        System.out.println("Module order: defaults, persistence, deduplication and new modules OK");
    }
}
