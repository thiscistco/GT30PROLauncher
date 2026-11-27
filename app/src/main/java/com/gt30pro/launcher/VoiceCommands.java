package com.gt30pro.launcher;

import java.util.Locale;

/** Narrow, explicit Indonesian voice grammar; never executes arbitrary text. */
final class VoiceCommands {
    static final int OFF=0, COMMAND=1, TYPING=2;
    static String body(int mode, String speech) {
        if (speech == null) return null;
        String value = speech.trim();
        String lower = value.toLowerCase(new Locale("id", "ID"));
        String prefix = mode == COMMAND ? "yo" : mode == TYPING ? "ketik" : "";
        if (prefix.isEmpty() || !lower.startsWith(prefix)) return null;
        int n=prefix.length();
        if (lower.length() > n && !Character.isWhitespace(lower.charAt(n))
                && lower.charAt(n)!=',' && lower.charAt(n)!=':') return null;
        while (n<value.length() && (Character.isWhitespace(value.charAt(n)) || value.charAt(n)==',' || value.charAt(n)==':')) n++;
        return value.substring(n).trim();
    }
    static String command(String text) {
        if (text == null) return "";
        String s=text.toLowerCase(new Locale("id", "ID")).trim().replaceAll("[.!?]+$", "").replaceAll("\\s+", " ");
        if (s.startsWith("buka ")) s=s.substring(5).trim();
        switch (s) {
            case "berhenti mendengarkan": case "matikan suara": return "stop";
            case "beranda": case "homescreen": case "home": return "home";
            case "super app": return "super";
            case "semua aplikasi": case "daftar aplikasi": return "drawer";
            case "jam": return "clock";
            case "alarm": return "alarm";
            case "jam dunia": return "world";
            case "timer": return "timer";
            case "stopwatch": return "stopwatch";
            case "kalender": return "calendar";
            case "cuaca": return "weather";
            case "jadwal mengajar": return "timetable";
            case "tambah acara": return "add_event";
            case "tambah alarm": return "add_alarm";
            case "hari ini": return "today";
            case "minggu berikutnya": case "bulan berikutnya": return "next_period";
            case "minggu sebelumnya": case "bulan sebelumnya": return "previous_period";
            case "mulai stopwatch": return "start_stopwatch";
            case "jeda stopwatch": return "pause_stopwatch";
            case "catat lap": return "lap";
            case "kembali": return "back";
            default: return "";
        }
    }
}
