package com.gt30pro.launcher;

/** Accumulate five dots, fade the last, then clear together. Negative phase means reduced motion. */
final class HomeClockPulse {
    static float intensity(float phase, int dot) {
        if (phase < 0f) return 1f;
        if (dot < 0 || dot >= 5) return 0f;
        float cycle = phase % 5f;
        if (cycle < dot) return 0f;
        if (dot == 4 && cycle > 4.5f) return (5f - cycle) * 2f;
        return 1f;
    }
    private HomeClockPulse() { }
}
