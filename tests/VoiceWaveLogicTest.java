package com.gt30pro.launcher;
import java.util.Arrays;

public final class VoiceWaveLogicTest {
    private static void check(boolean v) { if(!v) throw new AssertionError(); }
    public static void main(String[] args) {
        int[] rest={3,9,13,9,3};
        check(Arrays.equals(rest,VoiceWaveLogic.heights(0,0,true)));
        check(Arrays.equals(rest,VoiceWaveLogic.heights(1.5f,5,true)));
        check(Arrays.equals(rest,VoiceWaveLogic.heights(12,4,false)));
        check(Arrays.equals(rest,VoiceWaveLogic.heights(Float.NaN,4,true)));
        check(Arrays.equals(rest,VoiceWaveLogic.heights(Float.POSITIVE_INFINITY,4,true)));
        check(!Arrays.equals(VoiceWaveLogic.heights(9.5f,0,true),VoiceWaveLogic.heights(9.5f,1,true)));
        check(Arrays.equals(VoiceWaveLogic.heights(9.5f,3,true),VoiceWaveLogic.heights(900f,3,true)));
        for(int phase=0;phase<200;phase++) for(int h:VoiceWaveLogic.heights(7,phase,true)) check(h>=3 && h<=13 && h%2==1);
        int[] copy=VoiceWaveLogic.heights(0,0,false); copy[0]=0;
        check(VoiceWaveLogic.heights(0,0,false)[0]==3);
        System.out.println("Voice wave: silence, live levels, animation off and bounds OK");
    }
}
