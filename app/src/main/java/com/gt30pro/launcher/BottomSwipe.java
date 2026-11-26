package com.gt30pro.launcher;

/** Geometry shared by the home and Super App upward gestures. Coordinates use view pixels. */
final class BottomSwipe {
    private BottomSwipe() {}
    static boolean startsAtBottom(float y, float height) {
        return height > 0 && y >= height * .80f && y <= height;
    }
    static boolean shouldIntercept(float x0, float y0, float x, float y, float density) {
        float up = y0 - y;
        return up >= 12f * density && up > Math.abs(x - x0) * 1.5f;
    }
    static boolean completes(float x0, float y0, float x, float y, float height) {
        float up = y0 - y;
        return startsAtBottom(y0, height) && y <= height * .62f
                && up >= height * .28f && up > Math.abs(x - x0) * 1.5f;
    }
}
