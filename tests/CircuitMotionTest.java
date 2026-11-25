package com.gt30pro.launcher;

public class CircuitMotionTest {
    private static void check(float phase, float expected) {
        float actual = CircuitMotion.pingPong(phase);
        if (Math.abs(actual - expected) > 0.00001f)
            throw new AssertionError(phase + ": " + actual + " != " + expected);
    }
    public static void main(String[] args) {
        check(0f, 0f); check(.5f, .5f); check(.99f, .99f);
        check(1f, 1f); check(1.01f, .99f); check(1.5f, .5f);
        check(2f, 0f); check(2.01f, .01f); check(2.5f, .5f);
        check(-.5f, .5f);
        System.out.println("Circuit ping-pong motion tests passed");
    }
}
