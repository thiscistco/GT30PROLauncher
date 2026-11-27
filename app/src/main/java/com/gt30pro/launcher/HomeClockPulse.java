package com.gt30pro.launcher;

/** Accumulate five dots, fade the last, then clear together. Negative phase means reduced motion. */
final class HomeClockPulse {
    static float intensity(float phase, int dot) {
        if (phase < 0f) return 1f;
        if (dot < 0 || dot >= 5) return 0f;
        float cycle = phase % 6f;
        if (cycle < dot + .5f || cycle >= 5.5f) return 0f;
        if (dot == 4 && cycle > 5f) return (5.5f - cycle) * 2f;
        return 1f;
    }
    private HomeClockPulse() { }
}
