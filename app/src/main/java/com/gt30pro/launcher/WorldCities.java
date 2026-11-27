package com.gt30pro.launcher;

/** Selection limit independent of stored city indices and display order. */
final class WorldCities {
    static int count(boolean[] selected) {
        int count=0; for (boolean value : selected) if (value) count++; return count;
    }
    static boolean[] limit(boolean[] selected, int[] order, int maximum) {
        boolean[] result=new boolean[selected.length]; int count=0;
        for (int index : order) {
            if (index>=0 && index<selected.length && selected[index] && !result[index] && count<maximum) {
                result[index]=true; count++;
            }
        }
        return result;
    }
    private WorldCities() {}
}
