package com.gt30pro.launcher;

public class HomeClockPulseTest {
    private static void near(float actual,float expected) {
        if (Math.abs(actual-expected)>.00001f) throw new AssertionError("expected "+expected+" got "+actual);
    }
    public static void main(String[] args) {
        for (int dot=0;dot<5;dot++) {
            for (int i=0;i<5;i++) {
                near(HomeClockPulse.intensity(dot+.5f,i),i==dot ? 0f : 1f);
                near(HomeClockPulse.intensity(dot+5.5f,i),HomeClockPulse.intensity(dot+.5f,i));
            }
            near(HomeClockPulse.intensity(-1f,dot),1f);
            near(HomeClockPulse.intensity(dot,dot),1f);
            near(HomeClockPulse.intensity(dot+.25f,dot),.5f);
            near(HomeClockPulse.intensity(dot+.75f,dot),.5f);
        }
        System.out.println("Five-dot sequential fade out/in, loop and reduced motion: OK");
    }
}
