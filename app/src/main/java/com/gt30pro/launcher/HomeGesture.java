package com.gt30pro.launcher;

/** Homescreen-only vertical gestures; all measurements are physical view pixels. */
final class HomeGesture {
    static final int NONE=0, SUPER=1, NOTIFICATIONS=2, CONTROLS=3;
    private HomeGesture() { }
    static boolean vertical(float dx, float dy, float threshold) {
        return Math.abs(dy) >= threshold && Math.abs(dy) > Math.abs(dx) * 1.5f;
    }
    static int action(float startX, float startY, float x, float y, float width, float height, float density) {
        if (width<=0 || height<=0 || density<=0 || startX<0 || startX>width || startY<0 || startY>height) return NONE;
        float dx=x-startX, dy=y-startY;
        if (!vertical(dx,dy,48f*density)) return NONE;
        if (dy<0) return SUPER;
        return startX < width/2f ? NOTIFICATIONS : CONTROLS;
    }
}
