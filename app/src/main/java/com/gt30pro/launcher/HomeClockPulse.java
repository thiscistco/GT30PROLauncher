package com.gt30pro.launcher;

/** Five consecutive fade-out/fade-in cycles; negative phase represents reduced motion. */
final class HomeClockPulse {
    static float intensity(float phase, int dot) {
        if (phase < 0f) return 1f;
        float cycle = phase % 5f;
        if (dot < 0 || dot >= 5 || (int) cycle != dot) return 1f;
        float progress = cycle - dot;
        return Math.abs(1f - 2f * progress);
    }
    private HomeClockPulse() { }
}
