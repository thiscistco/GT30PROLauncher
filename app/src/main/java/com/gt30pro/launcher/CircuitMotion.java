package com.gt30pro.launcher;

/** Maps a two-way phase to a position along a path without endpoint jumps. */
final class CircuitMotion {
    private CircuitMotion() {}

    static float pingPong(float phase) {
        float cycle = phase % 2f;
        if (cycle < 0f) cycle += 2f;
        return cycle <= 1f ? cycle : 2f - cycle;
    }
}
