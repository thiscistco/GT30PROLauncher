package com.gt30pro.launcher;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Explicit controls and honest capability status, not a voice authentication screen. */
public final class LauncherSettingsActivity extends SwipeBackActivity {
    private AppUi ui;
    private Typeface font;
    private LinearLayout content;
    @Override protected boolean supportsBottomSwipe() { return true; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ui=new AppUi(this); ui.window(this);
        font=Typeface.createFromAsset(getAssets(),"fonts/Doto.ttf");
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(ui.canvas); scroll.setFillViewport(true);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets) -> {
            content.setPadding(dp(20),dp(16)+insets.getSystemWindowInsetTop(),dp(20),dp(24)+insets.getSystemWindowInsetBottom()); return insets;
        });
        content.addView(ui.header(this,font,"Setelan"));
        section("Launcher");
        button("Pilih launcher bawaan",() -> open(new Intent(Settings.ACTION_HOME_SETTINGS)));
        button("Lampu jalur: ubah hidup / mati",() -> {
            boolean enabled=getSharedPreferences("launcher_effects",MODE_PRIVATE).getBoolean("amber_glow",true);
            getSharedPreferences("launcher_effects",MODE_PRIVATE).edit().putBoolean("amber_glow",!enabled).apply();
            Toast.makeText(this,enabled?"Lampu jalur dimatikan":"Lampu jalur diaktifkan",Toast.LENGTH_SHORT).show();
        });
        section("Perizinan");
        text("Kelola izin mikrofon, notifikasi dan lokasi di info aplikasi Android. Beberapa izin hanya diminta saat fitur digunakan.");
        button("Buka izin aplikasi",() -> open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        section("Voice command dan voice typing");
        text("Perintah: ucapkan yo buka super app atau yo selesai untuk menghentikan mikrofon. Mengetik: awali dengan ketik, hanya di kolom launcher dan aplikasi bawaannya.");
        text("Pengenalan khusus suara Anda belum tersedia. Siapa pun yang terdengar dapat memicu perintah atau dikte. Jangan gunakan sebagai perlindungan akses; layanan pengenalan Android dapat mengirim audio ke penyedianya.");
        button("Mulai voice command",() -> VoiceController.start(this,VoiceCommands.COMMAND));
        button("Mulai voice typing",() -> VoiceController.start(this,VoiceCommands.TYPING));
        button("Hentikan suara",() -> VoiceController.stop(this));
        section("Output suara");
        text("Jawaban suara tersedia untuk pertanyaan kalender ada acara minggu ini, saat launcher terbuka dan perangkat tidak terkunci. Pasang suara Indonesia offline di mesin text-to-speech Android. Tidak semua perintah memiliki jawaban suara.");
        button("Setelan text-to-speech Android",() -> open(new Intent("com.android.settings.TTS_SETTINGS")));
        section("Bantuan AI lokal");
        text("AI lokal belum terintegrasi. Belum ada model yang bisa diunduh atau dipasang dari launcher ini. Pengenalan suara dan suara jawaban adalah layanan Android terpisah; memasang data suara tidak memasang AI. Bantuan unduh model akan tersedia setelah runtime dan model terintegrasi dan diverifikasi.");
    }
    @Override protected void onResume() { super.onResume(); AppUi current=new AppUi(this); if(ui!=null && (ui.dark!=current.dark || !ui.choice.equals(current.choice))) recreate(); }
    private int dp(int v) { return Math.round(v*getResources().getDisplayMetrics().density); }
    private void section(String title) {
        TextView t=new TextView(this); t.setText(title); t.setTypeface(font,Typeface.BOLD); t.setTextColor(ui.ink); t.setTextSize(18);
        t.setPadding(0,dp(22),0,dp(8)); content.addView(t);
    }
    private void text(String value) {
        TextView t=new TextView(this); t.setText(value); t.setTextColor(ui.muted); t.setTextSize(14); t.setTypeface(font);
        t.setPadding(0,dp(4),0,dp(10)); content.addView(t);
    }
    private void button(String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(ui.ink); b.setTypeface(font); b.setBackground(ui.surface(this,ui.control));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(4),0,dp(4)); content.addView(b,p);
        b.setMinHeight(dp(48)); b.setOnClickListener(v -> action.run());
    }
    private void open(Intent intent) {
        try { startActivity(intent); } catch (RuntimeException e) { Toast.makeText(this,"Setelan ini tidak tersedia di perangkat.",Toast.LENGTH_LONG).show(); }
    }
}
