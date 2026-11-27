package com.gt30pro.launcher;

import android.Manifest;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Jam bawaan: alarm, waktu dunia, timer dan stopwatch. */
public class ClockActivity extends SwipeBackActivity {
    @Override protected boolean supportsBottomSwipe() { return true; }
    private int AMBER, MUTED;
    private AppUi ui;
    private EditText timerHours, timerMinutes, timerSeconds;
    private String[] timerDraft = {"", "", ""};
    private static final String[] TAB_NAMES = {"Alarm", "Dunia", "Timer", "Stopwatch"};
    private static final int[] TAB_ICONS = {R.drawable.ic_clock_alarm, R.drawable.ic_clock_world,
            R.drawable.ic_clock_timer, R.drawable.ic_clock_stopwatch};
    private static final String[] CITIES = {"Jakarta", "Makassar", "Jayapura", "Singapore", "Tokyo", "Seoul", "Dubai", "London", "Paris", "New York", "Los Angeles", "Sydney"};
    private static final String[] ZONES = {"Asia/Jakarta", "Asia/Makassar", "Asia/Jayapura", "Asia/Singapore", "Asia/Tokyo", "Asia/Seoul", "Asia/Dubai", "Europe/London", "Europe/Paris", "America/New_York", "America/Los_Angeles", "Australia/Sydney"};
    private static final String[] DAYS = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Typeface doto;
    private LinearLayout content, tabs;
    private TextView timerDisplay, stopwatchDisplay;
    private LinearLayout worldList;
    private int tab = 0;
    private String editingSound = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
    private TextView soundLabel;
    private long swAccumulated, swStarted;
    private boolean swRunning;
    private JSONArray laps = new JSONArray();
    private long lastWorldSecond = -1;
    private boolean awaitingExactPermission;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateLive();
            handler.postDelayed(this, 50);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        restoreStopwatch();
        ui = new AppUi(this); AMBER=ui.ink; MUTED=ui.muted; ui.window(this);
        if (state != null) {
            tab = Math.max(0, Math.min(3, state.getInt("clock_tab",0)));
            String[] saved=state.getStringArray("timer_draft"); if (saved != null && saved.length == 3) timerDraft=saved;
        }
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(ui.canvas);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(16), dp(20), dp(16)); root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(20), dp(16) + insets.getSystemWindowInsetTop(), dp(20), dp(16) + insets.getSystemWindowInsetBottom()); return insets;
        });
        page.addView(ui.header(this, doto, "Jam"));
        HorizontalScrollView horizontal = new HorizontalScrollView(this); horizontal.setHorizontalScrollBarEnabled(false);
        tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); horizontal.addView(tabs);
        LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(-1, -2); tabParams.topMargin = dp(18); page.addView(horizontal, tabParams);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(false); page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0, dp(18), 0, dp(20)); scroll.addView(content);
        setContentView(root);
        showTab(tab);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        saveTimerDraft(); state.putInt("clock_tab",tab); state.putStringArray("timer_draft",timerDraft); super.onSaveInstanceState(state);
    }
    private void saveTimerDraft() {
        if (timerHours != null) timerDraft = new String[]{timerHours.getText().toString(), timerMinutes.getText().toString(), timerSeconds.getText().toString()};
    }
    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setTypeface(doto); v.setFontVariationSettings("'wght' 800"); return v;
    }
    private GradientDrawable bg(int color) {
        return ui.surface(this, color);
    }
    private TextView button(String label, Runnable click) {
        TextView v = text(label, 13, AMBER); v.setGravity(Gravity.CENTER); v.setPadding(dp(12), dp(12), dp(12), dp(12));
        v.setMinHeight(dp(48)); v.setBackground(bg(ui.control)); v.setOnClickListener(w -> click.run()); return v;
    }
    private void styleSwitch(Switch value) {
        ColorStateList tint = new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
                new int[]{ui.selected, ui.muted});
        value.setTextColor(ui.ink); value.setTypeface(doto); value.setThumbTintList(tint); value.setTrackTintList(tint);
    }
    private void addSpace(LinearLayout box, int dp) { android.view.View v = new android.view.View(this); box.addView(v, new LinearLayout.LayoutParams(1, dp(dp))); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void showTab(int selected) {
        saveTimerDraft(); timerHours = null; timerMinutes = null; timerSeconds = null;
        tab = selected; tabs.removeAllViews(); content.removeAllViews(); timerDisplay = null; stopwatchDisplay = null; worldList = null;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int index = i;
            ImageButton item = new ImageButton(this);
            item.setImageResource(TAB_ICONS[i]);
            item.setScaleType(ImageView.ScaleType.FIT_CENTER);
            item.setPadding(dp(13), dp(13), dp(13), dp(13));
            item.setBackground(bg(i == selected ? ui.selected : ui.control));
            item.setColorFilter(i == selected ? ui.selectedText : ui.ink);
            item.setContentDescription(TAB_NAMES[i]);
            item.setTooltipText(TAB_NAMES[i]);
            item.setSelected(i == selected);
            item.setOnClickListener(v -> showTab(index));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(52), dp(52));
            if (i > 0) p.leftMargin = dp(6);
            tabs.addView(item, p);
        }
        if (selected == 0) showAlarms(); else if (selected == 1) showWorld(); else if (selected == 2) showTimer(); else showStopwatch();
        updateLive();
    }
    private void requestAlerts() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41);
        else if (Build.VERSION.SDK_INT >= 24 && !((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled())
            Toast.makeText(this, "Aktifkan notifikasi launcher agar alarm dan timer berbunyi", Toast.LENGTH_LONG).show();
    }
    private boolean ensureExact() {
        if (ClockAlarms.canSchedule(this)) { requestAlerts(); return true; }
        ui.dialog(this).setTitle("Izin alarm tepat waktu")
                .setMessage("Aktifkan izin alarm & pengingat agar alarm dan timer dapat berbunyi tepat waktu. Setelah kembali, ulangi tindakan jika diperlukan.")
                .setPositiveButton("Buka setelan", (d, w) -> {
                    awaitingExactPermission = true;
                    try { startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()))); }
                    catch (Exception e) { awaitingExactPermission = false; Toast.makeText(this, "Buka izin alarm di setelan aplikasi", Toast.LENGTH_LONG).show(); }
                }).setNegativeButton("Nanti", null).show();
        return false;
    }
    private String dayLabel(int mask) {
        if (mask == 0) return "Sekali"; if (mask == 127) return "Setiap hari";
        StringBuilder b = new StringBuilder(); for (int i = 0; i < 7; i++) if ((mask & (1 << i)) != 0) { if (b.length() > 0) b.append(" · "); b.append(DAYS[i]); }
        return b.toString();
    }
    private void showAlarms() {
        content.addView(text("ALARM", 19, AMBER)); addSpace(content, 12);
        content.addView(button("+ Tambah alarm", () -> editAlarm(null))); addSpace(content, 16);
        JSONArray all = ClockAlarms.read(this);
        if (all.length() == 0) content.addView(text("Belum ada alarm. Tambahkan pengingat pertama.", 13, MUTED));
        for (int i = 0; i < all.length(); i++) {
            JSONObject a = all.optJSONObject(i); if (a == null) continue;
            LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(14),dp(12),dp(14),dp(12)); box.setBackground(bg(ui.card));
            TextView label = text(String.format(Locale.US, "%02d:%02d", a.optInt("hour"), a.optInt("minute")), 27, AMBER); box.addView(label);
            box.addView(text(dayLabel(a.optInt("days")) + (a.optBoolean("vibrate", true) ? "  ·  Getar" : ""), 12, MUTED));
            Switch enabled = new Switch(this); enabled.setText("Aktif"); styleSwitch(enabled); enabled.setChecked(a.optBoolean("enabled"));
            enabled.setOnCheckedChangeListener((v, checked) -> {
                if (checked && !ensureExact()) { v.setChecked(false); return; }
                try {
                    a.put("enabled", checked); ClockAlarms.put(this, a);
                    if (checked) ClockAlarms.schedule(this, a);
                    else {
                        ClockAlarms.cancel(this, a.optInt("id"));
                        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1000000 + a.optInt("id"));
                    }
                } catch (JSONException ignored) { }
            }); box.addView(enabled);
            LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.addView(button("Ubah", () -> editAlarm(a)), new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,-2,1); p.leftMargin=dp(8);
            actions.addView(button("Hapus", () -> ui.dialog(this).setMessage("Hapus alarm ini?").setPositiveButton("Hapus", (d,w) -> { ClockAlarms.remove(this, a.optInt("id")); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1000000 + a.optInt("id")); showTab(0); }).setNegativeButton("Batal",null).show()), p);
            box.addView(actions); content.addView(box); addSpace(content,10);
        }
    }
    private void editAlarm(JSONObject existing) {
        final int[] hour = {existing == null ? 7 : existing.optInt("hour",7)};
        final int[] minute = {existing == null ? 0 : existing.optInt("minute",0)};
        final int[] days = {existing == null ? 0 : existing.optInt("days",0)};
        editingSound = existing == null ? Settings.System.DEFAULT_ALARM_ALERT_URI.toString() : existing.optString("sound", Settings.System.DEFAULT_ALARM_ALERT_URI.toString());
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),0,dp(20),0);
        TextView time = button("", () -> {});
        Runnable updateTime = () -> time.setText(String.format(Locale.US, "Waktu  %02d:%02d", hour[0], minute[0])); updateTime.run();
        time.setOnClickListener(v -> new TimePickerDialog(this, ui.dark ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert, (picker,h,m) -> { hour[0]=h; minute[0]=m; updateTime.run(); }, hour[0],minute[0],true).show()); form.addView(time); addSpace(form,8);
        TextView repeat = button("Ulangi: " + dayLabel(days[0]), () -> {});
        repeat.setOnClickListener(v -> {
            boolean[] selected = new boolean[7]; for (int i=0;i<7;i++) selected[i]=(days[0] & (1<<i)) != 0;
            ui.dialog(this).setTitle("Pilih hari (kosong = sekali)").setMultiChoiceItems(DAYS, selected, (d,which,on)->selected[which]=on)
                    .setPositiveButton("OK", (d,w)-> { days[0]=0; for(int i=0;i<7;i++) if(selected[i]) days[0]|=1<<i; repeat.setText("Ulangi: " + dayLabel(days[0])); }).setNegativeButton("Batal",null).show();
        }); form.addView(repeat); addSpace(form,8);
        soundLabel = button("Nada: " + soundName(editingSound), () -> pickSound()); form.addView(soundLabel); addSpace(form,8);
        Switch vibrate = new Switch(this); styleSwitch(vibrate); vibrate.setText("Getar"); vibrate.setChecked(existing == null || existing.optBoolean("vibrate",true)); form.addView(vibrate);
        ui.dialog(this).setTitle(existing == null ? "Tambah alarm" : "Ubah alarm").setView(form)
                .setNegativeButton("Batal",null).setPositiveButton("Simpan", (d,w) -> {
                    if (!ensureExact()) return;
                    try {
                        JSONObject a = existing == null ? new JSONObject() : existing;
                        if (existing == null) a.put("id", ClockAlarms.nextId(this));
                        ClockAlarms.cancel(this,a.optInt("id"));
                        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1000000 + a.optInt("id"));
                        a.put("hour",hour[0]); a.put("minute",minute[0]); a.put("days",days[0]); a.put("sound",editingSound); a.put("vibrate",vibrate.isChecked()); a.put("enabled",true);
                        ClockAlarms.put(this,a);
                        if (!ClockAlarms.schedule(this,a)) Toast.makeText(this,"Alarm belum terjadwal. Periksa izin alarm.",Toast.LENGTH_LONG).show();
                        showTab(0);
                    } catch (JSONException ignored) { }
                }).show();
    }
    private String soundName(String value) {
        if (value.isEmpty()) return "Hening";
        try { Ringtone r = RingtoneManager.getRingtone(this,Uri.parse(value)); return r == null ? "Nada alarm" : r.getTitle(this); }
        catch (Exception e) { return "Nada alarm"; }
    }
    private void pickSound() {
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, editingSound.isEmpty() ? null : Uri.parse(editingSound));
        try { startActivityForResult(i, 42); } catch (Exception e) { Toast.makeText(this,"Pemilih nada tidak tersedia",Toast.LENGTH_SHORT).show(); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
        if (request == 42 && result == RESULT_OK && data != null) {
            Uri picked = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            editingSound = picked == null ? "" : picked.toString(); if (soundLabel != null) soundLabel.setText("Nada: " + soundName(editingSound));
        }
    }
    private boolean[] selectedCities() {
        boolean[] selected = new boolean[CITIES.length];
        String saved = ClockAlarms.prefs(this).getString("cities", "0,3,4,7,9");
        for (String s : saved.split(",")) try { int i=Integer.parseInt(s); if(i>=0 && i<selected.length) selected[i]=true; } catch(NumberFormatException ignored) { }
        return selected;
    }
    private void showWorld() {
        content.addView(text("JAM DUNIA", 19, AMBER)); addSpace(content,12);
        content.addView(button("Pilih kota", () -> {
            boolean[] chosen=selectedCities();
            ui.dialog(this).setTitle("Pilih kota").setMultiChoiceItems(CITIES,chosen,(d,i,on)->chosen[i]=on).setPositiveButton("Simpan",(d,w)->{
                StringBuilder b=new StringBuilder(); for(int i=0;i<chosen.length;i++) if(chosen[i]) { if(b.length()>0)b.append(',');b.append(i); }
                ClockAlarms.prefs(this).edit().putString("cities",b.toString()).apply(); showTab(1);
            }).setNegativeButton("Batal",null).show();
        })); addSpace(content,12);
        worldList = new LinearLayout(this); worldList.setOrientation(LinearLayout.VERTICAL); content.addView(worldList); lastWorldSecond=-1; updateWorld();
    }
    private void updateWorld() {
        if (worldList == null) return; worldList.removeAllViews(); boolean[] chosen=selectedCities(); Date now=new Date(); boolean any=false;
        for(int i=0;i<chosen.length;i++) if(chosen[i]) { any=true; SimpleDateFormat f=new SimpleDateFormat("HH:mm  ·  EEE, d MMM",new Locale("id","ID")); f.setTimeZone(TimeZone.getTimeZone(ZONES[i]));
            TextView row=text(CITIES[i]+"\n"+f.format(now),16,AMBER); row.setPadding(dp(14),dp(12),dp(14),dp(12)); row.setBackground(bg(ui.card)); worldList.addView(row); addSpace(worldList,8); }
        if(!any) worldList.addView(text("Pilih kota untuk ditampilkan.",13,MUTED));
    }
    private EditText number(String hint) {
        EditText v=new EditText(this); v.setSingleLine(true); v.setInputType(InputType.TYPE_CLASS_NUMBER); v.setHint(hint); v.setTextColor(AMBER); v.setHintTextColor(ui.muted); v.setBackground(bg(ui.card)); v.setPadding(dp(6),dp(8),dp(6),dp(8)); v.setTextSize(18); v.setGravity(Gravity.CENTER); v.setTypeface(doto); return v;
    }
    private long numberValue(EditText e) { try { return Long.parseLong(e.getText().toString()); } catch(Exception ignored) { return 0; } }
    private void showTimer() {
        content.addView(text("TIMER",19,AMBER)); addSpace(content,14);
        timerDisplay=text("00:00:00",32,AMBER); timerDisplay.setGravity(Gravity.CENTER); content.addView(timerDisplay); addSpace(content,12);
        LinearLayout fields=new LinearLayout(this); EditText h=number("Jam"),m=number("Menit"),s=number("Detik");
        timerHours=h; timerMinutes=m; timerSeconds=s; h.setText(timerDraft[0]); m.setText(timerDraft[1]); s.setText(timerDraft[2]);
        fields.addView(h,new LinearLayout.LayoutParams(0,-2,1));fields.addView(m,new LinearLayout.LayoutParams(0,-2,1));fields.addView(s,new LinearLayout.LayoutParams(0,-2,1));content.addView(fields);addSpace(content,12);
        content.addView(button("Mulai timer",()->{
            long hours=numberValue(h), minutes=numberValue(m), seconds=numberValue(s);
            if(hours>999 || minutes>59 || seconds>59 || (hours==0&&minutes==0&&seconds==0)) { Toast.makeText(this,"Masukkan durasi: menit dan detik 0–59",Toast.LENGTH_LONG).show();return; }
            if(!ensureExact())return; long end=System.currentTimeMillis()+(hours*3600+minutes*60+seconds)*1000;
            if(ClockAlarms.scheduleAt(this,ClockAlarms.TIMER_FIRE,0,end)){ClockAlarms.prefs(this).edit().putLong("timer_end",end).apply(); updateLive();}
            else Toast.makeText(this,"Timer belum terjadwal. Periksa izin alarm.",Toast.LENGTH_LONG).show();
        }));addSpace(content,8);
        content.addView(button("Batalkan timer",()->{ClockAlarms.cancelTimer(this); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(2000000); updateLive();}));
        addSpace(content,10); content.addView(text("Timer tetap berjalan saat halaman ditutup.",12,MUTED));
    }
    private long elapsed() { return swAccumulated + (swRunning ? SystemClock.elapsedRealtime()-swStarted : 0); }
    private String formatElapsed(long ms) { return String.format(Locale.US,"%02d:%02d:%02d.%03d",ms/3600000,(ms/60000)%60,(ms/1000)%60,ms%1000); }
    private void showStopwatch() {
        content.addView(text("STOPWATCH",19,AMBER));addSpace(content,14);
        stopwatchDisplay=text(formatElapsed(elapsed()),27,AMBER);stopwatchDisplay.setGravity(Gravity.CENTER);content.addView(stopwatchDisplay);addSpace(content,12);
        content.addView(button(swRunning ? "Jeda" : "Mulai",()->{
            if(swRunning){swAccumulated=elapsed();swRunning=false;}else{swStarted=SystemClock.elapsedRealtime();swRunning=true;} saveStopwatch();showTab(3);
        }));addSpace(content,8);
        LinearLayout row=new LinearLayout(this);row.addView(button("Lap",()->{ if(!swRunning)return;laps.put(elapsed());saveStopwatch();showTab(3); }),new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(8);row.addView(button("Reset",()->{swRunning=false;swAccumulated=0;swStarted=0;laps=new JSONArray();saveStopwatch();showTab(3);}),p);content.addView(row);addSpace(content,12);
        for(int i=laps.length()-1;i>=0;i--) { long total=laps.optLong(i); long prior=i>0?laps.optLong(i-1):0; TextView lap=text("Lap "+(i+1)+"   "+formatElapsed(total-prior)+"\nTotal  "+formatElapsed(total),13,MUTED);lap.setPadding(dp(8),dp(6),dp(8),dp(6));content.addView(lap); }
    }
    private int bootCount() { return Settings.Global.getInt(getContentResolver(),Settings.Global.BOOT_COUNT,0); }
    private void restoreStopwatch() {
        SharedPreferences p=ClockAlarms.prefs(this); swAccumulated=p.getLong("sw_acc",0); swStarted=p.getLong("sw_start",0);
        swRunning=p.getBoolean("sw_running",false) && p.getInt("sw_boot",-1)==bootCount();
        try{laps=new JSONArray(p.getString("sw_laps","[]"));}catch(JSONException ignored){laps=new JSONArray();}
    }
    private void saveStopwatch() { ClockAlarms.prefs(this).edit().putLong("sw_acc",swAccumulated).putLong("sw_start",swStarted).putBoolean("sw_running",swRunning).putInt("sw_boot",bootCount()).putString("sw_laps",laps.toString()).apply(); }
    private void updateLive() {
        if(stopwatchDisplay!=null) stopwatchDisplay.setText(formatElapsed(elapsed()));
        if(timerDisplay!=null){long end=ClockAlarms.prefs(this).getLong("timer_end",0);long seconds=Math.max(0,(end-System.currentTimeMillis()+999)/1000);timerDisplay.setText(String.format(Locale.US,"%02d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60));}
        long second=System.currentTimeMillis()/1000;if(worldList!=null&&second!=lastWorldSecond){lastWorldSecond=second;updateWorld();}
    }
    @Override protected void onResume() { super.onResume(); AppUi current=new AppUi(this); if (ui.dark != current.dark || !ui.choice.equals(current.choice)) { recreate(); return; } if(awaitingExactPermission){awaitingExactPermission=false;if(ClockAlarms.canSchedule(this))requestAlerts();} ClockAlarms.restore(this);handler.removeCallbacks(tick);tick.run(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
}
