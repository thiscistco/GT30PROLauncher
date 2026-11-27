package com.gt30pro.launcher;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.Editable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** UI bridge: microphone control and foreground-only safe operations. */
final class VoiceController {
    static final int AUDIO_PERMISSION=713;
    private static WeakReference<BottomSwipeActivity> active=new WeakReference<>(null);
    private static WeakReference<Dialog> dialog=new WeakReference<>(null);
    private static WeakReference<BottomSwipeActivity> dialogOwner=new WeakReference<>(null);
    private static int pendingMode=VoiceCommands.OFF;
    private static int wavePhase;
    // Weak keys avoid retaining destroyed pages or calendar headers after re-rendering.
    private static final WeakHashMap<ImageButton,Integer> buttons=new WeakHashMap<>();
    static void resume(BottomSwipeActivity activity) {
        active=new WeakReference<>(activity);
        if(dialogOwner.get()!=activity) { dialog.clear(); dialogOwner.clear(); }
    }
    static void pause(BottomSwipeActivity activity) {
        if (active.get()==activity) { active.clear(); resetWave(); }
    }
    static void watchDialog(BottomSwipeActivity activity, Dialog value) {
        dialogOwner=new WeakReference<>(activity); dialog=new WeakReference<>(value);
        value.setOnKeyListener((d, code, event) -> activity.handleVoiceVolumeKey(event));
    }
    static ImageButton button(BottomSwipeActivity a, int color) {
        ImageButton b=new ImageButton(a);
        b.setImageDrawable(new VoiceWaveDrawable(a));
        b.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int padding=Math.round(12*a.getResources().getDisplayMetrics().density);
        b.setPadding(padding,padding,padding,padding);
        b.setOnClickListener(v -> toggle(a,VoiceCommands.COMMAND));
        b.setOnLongClickListener(v -> { menu(a); return true; });
        setButtonColor(b,color); return b;
    }
    static void setButtonColor(ImageButton b,int color) { buttons.put(b,color); updateButton(b,color); }
    static void refreshButtons() {
        for (java.util.Map.Entry<ImageButton,Integer> entry : buttons.entrySet()) updateButton(entry.getKey(),entry.getValue());
    }
    static void voiceLevel(float rms) {
        BottomSwipeActivity foreground=active.get();
        if (foreground==null || VoiceService.mode==VoiceCommands.OFF) return;
        int phase=wavePhase++ & 1023;
        for (ImageButton b : buttons.keySet()) {
            if (b.getContext()==foreground && b.isShown() && b.getWindowVisibility()==View.VISIBLE && b.getDrawable() instanceof VoiceWaveDrawable)
                ((VoiceWaveDrawable)b.getDrawable()).setLevel(rms,phase);
        }
    }
    static void resetWave() {
        for (ImageButton b : buttons.keySet()) if (b.getDrawable() instanceof VoiceWaveDrawable) ((VoiceWaveDrawable)b.getDrawable()).reset();
    }
    private static void updateButton(ImageButton b,int color) {
        int mode=VoiceService.mode;
        boolean lightInk=android.graphics.Color.red(color)+android.graphics.Color.green(color)+android.graphics.Color.blue(color)>420;
        boolean home=b.getContext() instanceof MainActivity;
        int tint=home ? color : mode==VoiceCommands.COMMAND ? (lightInk ? 0xFFFFCB66 : 0xFF8C4800)
                : mode==VoiceCommands.TYPING ? (lightInk ? 0xFF9CE9FF : 0xFF005B7C) : color;
        b.setColorFilter(tint);
        android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(12*b.getResources().getDisplayMetrics().density);
        bg.setColor(home ? android.graphics.Color.TRANSPARENT : mode==VoiceCommands.COMMAND ? 0x33E79A24 : mode==VoiceCommands.TYPING ? 0x33379DC0 : android.graphics.Color.TRANSPARENT);
        b.setBackground(bg); b.setSelected(mode==VoiceCommands.COMMAND);
        String label=mode==VoiceCommands.COMMAND ? "Hentikan perintah suara" : "Aktifkan perintah suara";
        String state=mode==VoiceCommands.TYPING ? ". Mode mengetik aktif" : "";
        b.setContentDescription(label+state+". Tekan lama untuk pilihan suara");
        b.setTooltipText(label+" · tekan lama untuk pilihan suara");
    }
    static void menu(BottomSwipeActivity a) {
        String mode=VoiceService.mode==VoiceCommands.COMMAND ? "Perintah" : VoiceService.mode==VoiceCommands.TYPING ? "Ketik" : "Tidak aktif";
        new AlertDialog.Builder(a).setTitle("Kontrol suara · " + mode)
            .setMessage("Pendengaran berlanjut di latar belakang sampai dihentikan. Suara dapat dikirim ke penyedia pengenalan perangkat. Ucapkan yo, kalender, atau set untuk perintah; ketik untuk mendikte. Volume naik dua kali: perintah; turun dua kali: ketik.")
            .setItems(new String[]{"Mulai perintah · yo", "Mulai mengetik · ketik", "Hentikan suara"}, (d,which) -> {
                if (which==2) stop(a); else start(a, which==0 ? VoiceCommands.COMMAND : VoiceCommands.TYPING);
            }).setNegativeButton("Batal",null).show();
    }
    static void toggle(BottomSwipeActivity a, int mode) {
        if (VoiceService.mode==mode) stop(a); else start(a,mode);
    }
    static void start(BottomSwipeActivity a, int mode) {
        if (!a.getSharedPreferences("voice_consent",Context.MODE_PRIVATE).getBoolean("accepted",false)) {
            new AlertDialog.Builder(a).setTitle("Aktifkan mikrofon?")
                .setMessage("Mikrofon akan mendengarkan berulang kali, termasuk di latar belakang, sampai dihentikan atau Android menghentikannya. Suara dapat dikirim ke penyedia pengenalan suara perangkat. Notifikasi berisi tombol Hentikan. Dikte hanya di kolom launcher dan aplikasi bawaannya. Pengenalan pemilik belum tersedia: suara orang lain juga dapat menjalankan perintah.")
                .setNegativeButton("Batal",null).setPositiveButton("Lanjutkan",(d,w) -> {
                    a.getSharedPreferences("voice_consent",Context.MODE_PRIVATE).edit().putBoolean("accepted",true).apply(); start(a,mode);
                }).show();
            return;
        }
        boolean audio=a.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
        boolean notification=Build.VERSION.SDK_INT<33 || a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        if (!audio || !notification) {
            pendingMode=mode;
            if (Build.VERSION.SDK_INT>=33 && !notification && !audio) a.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS},AUDIO_PERMISSION);
            else a.requestPermissions(new String[]{audio ? Manifest.permission.POST_NOTIFICATIONS : Manifest.permission.RECORD_AUDIO},AUDIO_PERMISSION);
            return;
        }
        android.app.NotificationManager nm=(android.app.NotificationManager)a.getSystemService(Context.NOTIFICATION_SERVICE);
        android.app.NotificationChannel channel=nm.getNotificationChannel("voice_listening");
        if (!nm.areNotificationsEnabled() || (channel!=null && channel.getImportance()==android.app.NotificationManager.IMPORTANCE_NONE)) {
            Toast.makeText(a,"Aktifkan notifikasi launcher di setelan agar tombol Hentikan tersedia.",Toast.LENGTH_LONG).show(); return;
        }
        try {
            Intent i=new Intent(a,VoiceService.class).setAction(VoiceService.ACTION_START).putExtra("mode",mode);
            a.startForegroundService(i);
        } catch (RuntimeException e) { Toast.makeText(a,"Suara tidak dapat dimulai. Coba saat launcher terbuka.",Toast.LENGTH_LONG).show(); }
    }
    static void permissionResult(BottomSwipeActivity a, int requestCode) {
        if (requestCode!=AUDIO_PERMISSION) return;
        int requested=pendingMode; pendingMode=VoiceCommands.OFF;
        boolean audio=a.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;
        boolean notification=Build.VERSION.SDK_INT<33 || a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        if (requested!=VoiceCommands.OFF && audio && notification) start(a,requested);
        else Toast.makeText(a,"Izin mikrofon dan notifikasi diperlukan. Aktifkan di setelan aplikasi.",Toast.LENGTH_LONG).show();
    }
    static void stop(Context c) { c.stopService(new Intent(c,VoiceService.class)); }
    static boolean foregroundUnlocked(Context c) {
        BottomSwipeActivity a=active.get();
        android.app.KeyguardManager lock=(android.app.KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE);
        if (a==null || a.isFinishing() || a.isDestroyed() || lock==null || lock.isDeviceLocked()) return false;
        Dialog d=dialog.get();
        return a.getWindow().getDecorView().hasWindowFocus() || (d!=null && d.isShowing() && d.getWindow()!=null && d.getWindow().getDecorView().hasWindowFocus());
    }
    static Intent destination(Context c,String action) {
        Class<?> target;
        switch (action) {
            case "home": target=MainActivity.class; break;
            case "super": target=SuperAppActivity.class; break;
            case "drawer": target=AppDrawerActivity.class; break;
            case "calendar": case "add_event": target=CalendarActivity.class; break;
            case "weather": target=WeatherActivity.class; break;
            case "timetable": target=TimeTableActivity.class; break;
            case "clock": case "alarm": case "world": case "timer": case "stopwatch": case "add_alarm": target=ClockActivity.class; break;
            default: return null;
        }
        Intent i=new Intent(c,target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (action.equals("alarm") || action.equals("world") || action.equals("timer") || action.equals("stopwatch") || action.equals("add_alarm") || action.equals("add_event")) i.putExtra("voice_action",action);
        return i;
    }
    static boolean deliver(int mode, String phrase) {
        String body=VoiceCommands.body(mode,phrase);
        if (body==null) return true; // unrelated speech is ignored
        String stop=VoiceCommands.command(body);
        if (mode==VoiceCommands.COMMAND && "stop".equals(stop)) { BottomSwipeActivity a=active.get(); if(a!=null) stop(a); return false; }
        BottomSwipeActivity a=active.get();
        if (a==null || a.isFinishing() || a.isDestroyed()) return false;
        Dialog visible=dialog.get();
        boolean dialogFocused=visible!=null && visible.isShowing() && visible.getWindow()!=null && visible.getWindow().getDecorView().hasWindowFocus();
        if (!dialogFocused && !a.getWindow().getDecorView().hasWindowFocus()) return false;
        if (mode==VoiceCommands.TYPING) { insert(a,body); return true; }
        if (dialogFocused) { Toast.makeText(a,"Tutup dialog untuk menjalankan perintah.",Toast.LENGTH_SHORT).show(); return true; }
        if (stop.isEmpty()) { Toast.makeText(a,"Perintah tidak dikenal.",Toast.LENGTH_SHORT).show(); return true; }
        if ("back".equals(stop)) { a.onBackPressed(); return true; }
        if (a.handleVoiceAction(stop)) return true;
        Intent destination=destination(a,stop);
        if (destination!=null) a.startActivity(destination);
        else Toast.makeText(a,"Perintah ini digunakan di halaman terkait.",Toast.LENGTH_SHORT).show();
        return true;
    }
    private static boolean textField(View v) {
        if (!(v instanceof EditText) || !v.isShown() || !v.isEnabled() || !v.isFocusable()
                || !v.getGlobalVisibleRect(new android.graphics.Rect())) return false;
        EditText e=(EditText)v;
        if (e.getTransformationMethod() instanceof android.text.method.PasswordTransformationMethod) return false;
        int input=e.getInputType(), kind=input & InputType.TYPE_MASK_CLASS;
        int variant=input & InputType.TYPE_MASK_VARIATION;
        return kind==InputType.TYPE_CLASS_TEXT && variant!=InputType.TYPE_TEXT_VARIATION_PASSWORD
                && variant!=InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD && variant!=InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD;
    }
    private static EditText first(View v) {
        if (v==null || !v.isShown()) return null;
        if (textField(v)) return (EditText)v;
        if (v instanceof ViewGroup) { ViewGroup g=(ViewGroup)v; for(int i=0;i<g.getChildCount();i++) { EditText result=first(g.getChildAt(i)); if(result!=null)return result; } }
        return null;
    }
    private static void insert(BottomSwipeActivity a,String value) {
        Dialog d=dialog.get(); View root=null, focused=null;
        if (d!=null && d.isShowing() && d.getWindow()!=null && d.getWindow().getDecorView().hasWindowFocus()) {
            root=d.getWindow().getDecorView(); focused=d.getCurrentFocus();
        } else if (a.getWindow().getDecorView().hasWindowFocus()) {
            root=a.getWindow().getDecorView(); focused=a.getCurrentFocus();
        }
        if (root==null) { Toast.makeText(a,"Buka kolom teks launcher untuk mengetik.",Toast.LENGTH_SHORT).show(); return; }
        // Never redirect dictation away from a focused sensitive/non-text field.
        if (focused instanceof EditText && !textField(focused)) { Toast.makeText(a,"Kolom ini tidak mendukung dikte.",Toast.LENGTH_SHORT).show(); return; }
        EditText field=textField(focused)?(EditText)focused:first(root);
        if(field==null) { Toast.makeText(a,"Tidak ada kolom teks yang tersedia.",Toast.LENGTH_SHORT).show(); return; }
        boolean wasFocused=field.hasFocus(); field.requestFocus();
        if (!wasFocused) field.setSelection(field.length());
        if (value.isEmpty()) return;
        Editable text=field.getText(); int start=field.getSelectionStart(), end=field.getSelectionEnd();
        if(start<0 || end<0) start=end=text.length();
        int lo=Math.min(start,end), hi=Math.max(start,end);
        if (lo>0 && !Character.isWhitespace(text.charAt(lo-1)) && !Character.isWhitespace(value.charAt(0))) value=" "+value;
        text.replace(lo,hi,value);
    }
}
