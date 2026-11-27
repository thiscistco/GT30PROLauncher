package com.gt30pro.launcher;

public class HomeClockPulseTest {
    public static void main(String[] args) {
        for (int dot=0;dot<5;dot++) {
            for (int i=0;i<5;i++) {
                float pulse=HomeClockPulse.intensity(dot+.5f,i);
                if (Math.abs(pulse-(i==dot ? 1f : 0f))>.00001f) throw new AssertionError("wrong dot at "+dot+" "+i);
                if (Math.abs(HomeClockPulse.intensity(dot+5.5f,i)-pulse)>.00001f) throw new AssertionError("loop");
            }
            if (HomeClockPulse.intensity(-1f,dot)!=1f) throw new AssertionError("reduced motion");
            if (HomeClockPulse.intensity(dot+.1f,dot)>=HomeClockPulse.intensity(dot+.5f,dot)) throw new AssertionError("pulse");
        }
        System.out.println("Five-dot sequential heartbeat, loop and reduced motion: OK");
    }
}
