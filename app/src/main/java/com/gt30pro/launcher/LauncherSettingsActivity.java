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
import android.widget.Switch;
import android.os.Handler;
import android.os.Looper;

/** Explicit controls and honest capability status, not a voice authentication screen. */
public final class LauncherSettingsActivity extends SwipeBackActivity {
    private AppUi ui;
    private Typeface font;
    private LinearLayout content;
    private TextView modelStatus;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable updateModel=new Runnable() { public void run() {
        if(modelStatus!=null) modelStatus.setText(ModelDownload.status(LauncherSettingsActivity.this));
        handler.postDelayed(this,1000);
    }};
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
        Switch glow=new Switch(this); glow.setText("Animasi lampu jalur"); glow.setTextColor(ui.ink); glow.setTypeface(font);
        glow.setMinHeight(dp(48)); glow.setChecked(getSharedPreferences("launcher_effects",MODE_PRIVATE).getBoolean("amber_glow",true));
        glow.setOnCheckedChangeListener((v,checked) -> getSharedPreferences("launcher_effects",MODE_PRIVATE).edit().putBoolean("amber_glow",checked).apply());
        content.addView(glow);
        button("Tema aplikasi",() -> ui.dialog(this).setTitle("Tema aplikasi").setItems(new String[]{"Ikuti perangkat","Terang","Gelap"},(d,which) -> {
            String[] values={"system","light","dark"}; getSharedPreferences("calendar_appearance",MODE_PRIVATE).edit().putString("theme",values[which]).apply(); recreate();
        }).show());
        button("Izin kunci layar double tap",() -> {
            android.content.ComponentName admin=new android.content.ComponentName(this,ScreenLockAdmin.class);
            android.app.admin.DevicePolicyManager policy=(android.app.admin.DevicePolicyManager)getSystemService(DEVICE_POLICY_SERVICE);
            if(policy!=null && policy.isAdminActive(admin)) {
                ui.dialog(this).setMessage("Cabut izin kunci layar? Double tap tidak akan mengunci sampai diaktifkan kembali.")
                    .setNegativeButton("Batal",null).setPositiveButton("Cabut",(d,w) -> policy.removeActiveAdmin(admin)).show();
            } else open(new Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,admin)
                    .putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,"Opsional: mengunci layar dengan double tap di homescreen."));
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
        section("Model AI lokal");
        text("Qwen3 0.6B Q8 dari Qwen · 639 MB. Model diverifikasi SHA-256 sebelum disimpan. Runtime AI belum terintegrasi, sehingga model belum dapat digunakan untuk voice command.");
        modelStatus=new TextView(this); modelStatus.setTextColor(ui.muted); modelStatus.setTypeface(font); modelStatus.setTextSize(14); content.addView(modelStatus);
        button("Unduh model AI",() -> ui.dialog(this).setTitle("Unduh model 639 MB?")
            .setMessage("Mengunduh model resmi dari Hugging Face memakai internet dan ruang penyimpanan (sekitar 740 MB kosong). Server menerima alamat IP Anda. Biarkan aplikasi terbuka; jika proses terhenti ulangi unduhan. Model belum dapat dipakai sampai runtime AI tersedia.")
            .setNegativeButton("Batal",null).setPositiveButton("Unduh",(d,w) -> { ModelDownload.start(this); modelStatus.setText(ModelDownload.status(this)); }).show());
        button("Batalkan unduhan model",() -> { ModelDownload.cancel(); modelStatus.setText(ModelDownload.status(this)); });
        button("Hapus model AI",() -> ui.dialog(this).setMessage("Hapus model yang diunduh?")
            .setNegativeButton("Batal",null).setPositiveButton("Hapus",(d,w) -> { if(!ModelDownload.delete(this)) Toast.makeText(this,"Batalkan unduhan terlebih dahulu.",Toast.LENGTH_LONG).show(); modelStatus.setText(ModelDownload.status(this)); }).show());
    }
    @Override protected void onResume() { super.onResume(); AppUi current=new AppUi(this); if(ui!=null && (ui.dark!=current.dark || !ui.choice.equals(current.choice))) { recreate(); return; } handler.removeCallbacks(updateModel); handler.post(updateModel); }
    @Override protected void onPause() { handler.removeCallbacks(updateModel); super.onPause(); }
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
