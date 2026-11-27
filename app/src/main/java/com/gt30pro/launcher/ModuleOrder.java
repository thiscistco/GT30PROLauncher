package com.gt30pro.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Validate persisted order, discard duplicates/unknowns, append newly added modules. */
final class ModuleOrder {
    static final List<String> DEFAULT=Arrays.asList("clock","calendar","weather","timetable","settings","notes");
    static ArrayList<String> parse(String saved) {
        ArrayList<String> result=new ArrayList<>();
        if(saved!=null) for(String id:saved.split(",")) if(DEFAULT.contains(id) && !result.contains(id)) result.add(id);
        for(String id:DEFAULT) if(!result.contains(id)) result.add(id);
        return result;
    }
    private ModuleOrder() { }
}
