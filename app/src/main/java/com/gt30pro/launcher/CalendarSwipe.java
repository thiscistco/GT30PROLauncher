package com.gt30pro.launcher;
/** Horizontal month gesture; vertical scrolling and short taps do not navigate. */
final class CalendarSwipe {
    static int direction(float dx, float dy, float threshold) {
        if (Math.abs(dx) < threshold || Math.abs(dx) <= Math.abs(dy) * 1.5f) return 0;
        return dx < 0 ? 1 : -1;
    }
    static boolean expand(float dx,float dy,float threshold) { return dy>=threshold && dy>Math.abs(dx)*1.5f; }
    private CalendarSwipe() { }
}
