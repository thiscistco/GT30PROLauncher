package com.gt30pro.launcher;

/** Five consecutive one-second pulses; negative phase represents reduced motion. */
final class HomeClockPulse {
    static float intensity(float phase, int dot) {
        if (phase < 0f) return 1f;
        float cycle = phase % 5f;
        if (dot < 0 || dot >= 5 || (int) cycle != dot) return 0f;
        return (float) Math.sin(Math.PI * (cycle - dot));
    }
    private HomeClockPulse() { }
}
