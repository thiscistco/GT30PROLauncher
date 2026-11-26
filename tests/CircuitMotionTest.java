package com.gt30pro.launcher;

public class CircuitMotionTest {
    private static void check(float phase, float expected) {
        float actual = CircuitMotion.pingPong(phase);
        if (Math.abs(actual - expected) > 0.00001f)
            throw new AssertionError(phase + ": " + actual + " != " + expected);
    }
    private static void checkDouble(String name, double actual, double expected) {
        if (Math.abs(actual - expected) > 0.00001) throw new AssertionError(name + ": " + actual + " != " + expected);
    }
    public static void main(String[] args) {
        check(0f, 0f); check(.5f, .5f); check(.99f, .99f);
        check(1f, 1f); check(1.01f, .99f); check(1.5f, .5f);
        check(2f, 0f); check(2.01f, .01f); check(2.5f, .5f);
        check(-.5f, .5f);
        checkDouble("RAM 0%", CircuitMotion.usedRamFraction(1000, 1000), 0);
        checkDouble("RAM 50%", CircuitMotion.usedRamFraction(1000, 500), 0.5);
        checkDouble("RAM 100%", CircuitMotion.usedRamFraction(1000, 0), 1);
        checkDouble("RAM fallback", CircuitMotion.usedRamFraction(0, 0), 0.5);
        checkDouble("RAM clamped", CircuitMotion.usedRamFraction(1000, 1200), 0);
        checkDouble("slow", CircuitMotion.durationMs(6000, 0), 9000);
        checkDouble("normal", CircuitMotion.durationMs(6000, 0.5), 6000);
        checkDouble("fast", CircuitMotion.durationMs(6000, 1), 3000);
        checkDouble("invalid memory", CircuitMotion.durationMs(6000, Double.NaN), 6000);
        double phase = CircuitMotion.advance(0, 1200, CircuitMotion.durationMs(6000, .5));
        checkDouble("first movement", phase, .2);
        checkDouble("no position jump when speed changes", CircuitMotion.advance(phase, 0, CircuitMotion.durationMs(6000, 1)), .2);
        phase = CircuitMotion.advance(phase, 300, CircuitMotion.durationMs(6000, 1));
        checkDouble("faster movement", phase, .3);
        checkDouble("wrap after return", CircuitMotion.advance(1.9, 300, 3000), 0);
        System.out.println("Circuit ping-pong, RAM speed and phase continuity tests passed");
    }
}
