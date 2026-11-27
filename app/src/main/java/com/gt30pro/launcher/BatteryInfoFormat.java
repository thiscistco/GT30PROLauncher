package com.gt30pro.launcher;

import java.util.Locale;

/** Formats only reported Android battery properties; never infers battery health or capacity. */
final class BatteryInfoFormat {
    static final String UNKNOWN="Tidak tersedia";
    private static String number(String format,double value) { return String.format(Locale.US,format,value).replace('.',','); }
    static String status(int value) {
        switch(value) { case 2: return "Mengisi"; case 3: return "Digunakan"; case 4: return "Tidak mengisi"; case 5: return "Penuh"; default: return UNKNOWN; }
    }
    static String source(int value) {
        if(value<0) return UNKNOWN;
        if(value==0) return "Tidak terhubung";
        String s="";
        if((value&1)!=0) s="AC";
        if((value&2)!=0) s+=(s.isEmpty()?"":" + ")+"USB";
        if((value&4)!=0) s+=(s.isEmpty()?"":" + ")+"Nirkabel";
        if((value&8)!=0) s+=(s.isEmpty()?"":" + ")+"Dock";
        return s.isEmpty() || (value&~15)!=0 ? UNKNOWN : s;
    }
    static String health(int value) {
        switch(value) { case 2: return "Baik"; case 3: return "Terlalu panas"; case 4: return "Rusak"; case 5: return "Tegangan tinggi"; case 6: return "Gangguan"; case 7: return "Terlalu dingin"; default: return UNKNOWN; }
    }
    static String level(int level,int scale) {
        if(level<0 || scale<=0 || level>scale) return UNKNOWN;
        return (int)((long)level*100/scale)+"%";
    }
    static String temperature(int tenths) {
        return tenths==Integer.MIN_VALUE || tenths < -500 || tenths > 1500 ? UNKNOWN : number("%.1f",tenths/10.0)+" °C";
    }
    static String voltage(int millivolts) {
        return millivolts<=0 || millivolts>100000 ? UNKNOWN : number("%.3f",millivolts/1000.0)+" V";
    }
    static String current(int microamps) {
        return microamps==Integer.MIN_VALUE ? UNKNOWN : number("%+.0f",microamps/1000.0)+" mA";
    }
    static String charge(int microampHours) {
        return microampHours<0 ? UNKNOWN : number("%.0f",microampHours/1000.0)+" mAh";
    }
    static String energy(long nanowattHours) {
        return nanowattHours<0 ? UNKNOWN : number("%.2f",nanowattHours/1000000000.0)+" Wh";
    }
    static String timeToFull(long milliseconds) {
        if(milliseconds<0) return UNKNOWN;
        long minutes=milliseconds/60000 + (milliseconds%60000==0 ? 0 : 1);
        if(minutes>=60) return (minutes/60)+" jam "+(minutes%60)+" menit";
        return minutes+" menit";
    }
    static String cycles(int value) { return value<0 ? UNKNOWN : String.valueOf(value); }
    static String technology(String value) {
        if(value==null || value.trim().isEmpty()) return UNKNOWN;
        String s=value.replaceAll("[\\p{Cntrl}]", " ").trim();
        return s.isEmpty() ? UNKNOWN : s.substring(0,Math.min(s.length(),32));
    }
    static String present(int value) { return value==1 ? "Ya" : value==0 ? "Tidak" : UNKNOWN; }
    static String low(int value) { return value==1 ? "Ya" : value==0 ? "Tidak" : UNKNOWN; }
    private BatteryInfoFormat() { }
}
