package com.gt30pro.launcher;

/** Maps a two-way phase to a position along a path without endpoint jumps. */
final class CircuitMotion {
    private CircuitMotion() {}

    /** Android's available memory includes memory the system can reclaim. */
    static double usedRamFraction(long total, long available) {
        if (total <= 0) return 0.5;
        return Math.max(0.0, Math.min(1.0, 1.0 - (double) available / total));
    }

    /** One-way duration: 150% at 0% RAM, 100% at 50%, 50% at 100%. */
    static double durationMs(long baseMs, double usedFraction) {
        if (Double.isNaN(usedFraction)) usedFraction = 0.5;
        usedFraction = Math.max(0.0, Math.min(1.0, usedFraction));
        return Math.max(1L, baseMs) * (1.5 - usedFraction);
    }

    /** Integrate time rather than restarting animation when the speed changes. */
    static double advance(double phase, long elapsedMs, double durationMs) {
        if (elapsedMs > 0 && durationMs > 0) phase += elapsedMs / durationMs;
        phase %= 2.0;
        return phase < 0 ? phase + 2.0 : phase;
    }

    static float pingPong(float phase) {
        float cycle = phase % 2f;
        if (cycle < 0f) cycle += 2f;
        return cycle <= 1f ? cycle : 2f - cycle;
    }
}
