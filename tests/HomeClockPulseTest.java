package com.gt30pro.launcher;

public class HomeClockPulseTest {
    private static void near(float actual,float expected) {
        if (Math.abs(actual-expected)>.00001f) throw new AssertionError("expected "+expected+" got "+actual);
    }
    public static void main(String[] args) {
        for (int dot=0;dot<5;dot++) {
            near(HomeClockPulse.intensity(-1f,dot),1f);
            for (int step=0;step<5;step++) {
                float phase=step+.25f, expected=dot<=step ? 1f : 0f;
                near(HomeClockPulse.intensity(phase,dot),expected);
                near(HomeClockPulse.intensity(phase+5f,dot),expected);
            }
            near(HomeClockPulse.intensity(4.75f,dot),dot==4 ? .5f : 1f);
            near(HomeClockPulse.intensity(4.999f,dot),dot==4 ? .002f : 1f);
            near(HomeClockPulse.intensity(5f,dot),dot==0 ? 1f : 0f);
            near(HomeClockPulse.intensity(5.001f,dot),dot==0 ? 1f : 0f);
        }
        near(HomeClockPulse.intensity(3f,-1),0f);
        near(HomeClockPulse.intensity(3f,5),0f);
        System.out.println("Five accumulating dots: last fades and first restarts immediately, reduced motion: OK");
    }
}
