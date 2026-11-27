package com.gt30pro.launcher;

/** Converts recognizer RMS callbacks to bounded dot-column heights. No audio is stored. */
final class VoiceWaveLogic {
    private static final int[] REST={3,9,13,9,3};
    static int[] heights(float rms,int phase,boolean enabled) {
        if (!enabled || Float.isNaN(rms) || Float.isInfinite(rms) || rms<=1.5f) return REST.clone();
        float strength=Math.max(0f,Math.min(1f,(rms-1.5f)/8f));
        int[] counts=new int[5];
        for (int i=0;i<counts.length;i++) {
            double wave=.5+.5*Math.sin(phase*.85+i*1.25);
            int half=Math.max(1,Math.min(6,Math.round(1f+5f*strength*(float)wave)));
            counts[i]=half*2+1;
        }
        return counts;
    }
}
