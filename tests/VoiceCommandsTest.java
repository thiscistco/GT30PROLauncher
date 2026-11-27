package com.gt30pro.launcher;

public class VoiceCommandsTest {
    private static void eq(Object expected,Object actual) { if(expected==null ? actual!=null : !expected.equals(actual)) throw new AssertionError("expected="+expected+" actual="+actual); }
    public static void main(String[] args) {
        eq("buka Kalender",VoiceCommands.body(VoiceCommands.COMMAND," Yo, buka Kalender "));
        eq(null,VoiceCommands.body(VoiceCommands.COMMAND,"yoga kalender"));
        eq(null,VoiceCommands.body(VoiceCommands.COMMAND,"ketik kalender"));
        eq(null,VoiceCommands.body(VoiceCommands.TYPING,"yo buka timer"));
        eq("Halo dunia",VoiceCommands.body(VoiceCommands.TYPING,"Ketik: Halo dunia"));
        eq(null,VoiceCommands.body(VoiceCommands.TYPING,"ketikan sesuatu"));
        eq("calendar",VoiceCommands.command("Buka KALENDER."));
        eq("timetable",VoiceCommands.command("buka jadwal mengajar"));
        eq("timer",VoiceCommands.command("buka timer"));
        eq("stop",VoiceCommands.command("berhenti mendengarkan"));
        eq("",VoiceCommands.command("hapus semua acara"));
        eq("",VoiceCommands.command("buka kalender lalu hapus"));
        VoiceVolumeShortcut v=new VoiceVolumeShortcut();
        eq(false,v.press(1,100)); eq(true,v.press(1,700)); eq(false,v.press(1,701));
        eq(false,v.press(1,1400)); eq(false,v.press(2,1450)); eq(false,v.press(1,1500));
        eq(true,v.press(1,1550)); eq(false,v.press(1,2000)); eq(false,v.press(1,1999));
        System.out.println("Voice grammar and volume shortcut OK");
    }
}
