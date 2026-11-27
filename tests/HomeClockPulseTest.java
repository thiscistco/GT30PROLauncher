package com.gt30pro.launcher;

public class HomeClockPulseTest {
    private static void near(float actual,float expected) {
        if (Math.abs(actual-expected)>.00001f) throw new AssertionError("expected "+expected+" got "+actual);
    }
    public static void main(String[] args) {
        for (int dot=0;dot<5;dot++) {
            near(HomeClockPulse.intensity(-1f,dot),1f);
            near(HomeClockPulse.intensity(0.25f,dot),0f);
            for (int step=0;step<5;step++) {
                float phase=step+.75f, expected=dot<=step ? 1f : 0f;
                near(HomeClockPulse.intensity(phase,dot),expected);
                near(HomeClockPulse.intensity(phase+6f,dot),expected);
            }
            near(HomeClockPulse.intensity(5.25f,dot),dot==4 ? .5f : 1f);
            near(HomeClockPulse.intensity(5.5f,dot),0f);
            near(HomeClockPulse.intensity(5.9f,dot),0f);
            near(HomeClockPulse.intensity(6.25f,dot),0f);
        }
        near(HomeClockPulse.intensity(3f,-1),0f);
        near(HomeClockPulse.intensity(3f,5),0f);
        System.out.println("Five dots accumulate and stay, last fades then clear; loop and reduced motion: OK");
    }
}
