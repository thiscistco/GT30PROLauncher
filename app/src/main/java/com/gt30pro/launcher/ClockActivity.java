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
import android.view.View;
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
import java.util.Calendar;
import java.util.ArrayList;

/** Jam bawaan: alarm, waktu dunia, timer dan stopwatch. */
public class ClockActivity extends SwipeBackActivity {
    @Override protected boolean supportsBottomSwipe() { return true; }
    private int AMBER, MUTED;
    private AppUi ui;
    private EditText timerHours, timerMinutes, timerSeconds;
    private String[] timerDraft = {"", "", ""};
    private static final String[] TAB_NAMES = {"Alarm", "Jam Dunia", "Timer", "Stopwatch"};
    private static final int[] TAB_ICONS = {R.drawable.ic_clock_alarm, R.drawable.ic_clock_world,
            R.drawable.ic_clock_timer, R.drawable.ic_clock_stopwatch};
    private static final String[] CITIES = {"Jakarta", "Makassar", "Jayapura", "Singapore", "Tokyo", "Seoul", "Dubai", "London", "Paris", "New York", "Los Angeles", "Sydney"};
    private static final String[] ZONES = {"Asia/Jakarta", "Asia/Makassar", "Asia/Jayapura", "Asia/Singapore", "Asia/Tokyo", "Asia/Seoul", "Asia/Dubai", "Europe/London", "Europe/Paris", "America/New_York", "America/Los_Angeles", "Australia/Sydney"};
    private static final String[] DAYS = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Typeface doto;
    private LinearLayout content, tabs;
    private HorizontalScrollView tabsScroll;
    private FrameLayout stage;
    private ClockHudView hud;
    private ScrollView panelScroll;
    private long lastHudSecond = -1;
    private TextView timerDisplay, stopwatchDisplay;
    private LinearLayout worldList;
    private int tab = -1;
    private String editingSound = Settings.System.DEFAULT_ALARM_ALERT_URI.toString();
    private ImageButton soundButton;
    private LinearLayout alarmEditor;
    private long swAccumulated, swStarted;
    private boolean swRunning;
    private JSONArray laps = new JSONArray();
    private long lastWorldSecond = -1, lastAlarmSecond = -1;
    private final ArrayList<AlarmRow> alarmRows = new ArrayList<>();
    private static final class AlarmRow {
        final int id; final TextView countdown; final Switch enabled; boolean updating;
        AlarmRow(int id, TextView countdown, Switch enabled) { this.id=id; this.countdown=countdown; this.enabled=enabled; }
    }
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
            tab = Math.max(-1, Math.min(3, state.getInt("clock_tab",-1)));
            String[] saved=state.getStringArray("timer_draft"); if (saved != null && saved.length == 3) timerDraft=saved;
        }
        FrameLayout root = new FrameLayout(this); root.setBackgroundColor(ui.canvas);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(16), dp(20), dp(16)); root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(20), dp(16) + insets.getSystemWindowInsetTop(), dp(20), dp(16) + insets.getSystemWindowInsetBottom()); return insets;
        });
        LinearLayout header=ui.header(this, doto, "Jam");
        header.getChildAt(0).setOnClickListener(v -> onBackPressed()); page.addView(header);
        stage = new FrameLayout(this); page.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
        hud = new ClockHudView(this,ui,doto); stage.addView(hud,new FrameLayout.LayoutParams(-1,-1));
        panelScroll = new ScrollView(this); panelScroll.setFillViewport(false);
        FrameLayout.LayoutParams panelParams=new FrameLayout.LayoutParams(-1,-1); panelParams.leftMargin=dp(16); panelParams.rightMargin=dp(16); panelParams.bottomMargin=dp(16);
        stage.addView(panelScroll,panelParams);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(0, dp(8), 0, dp(20)); panelScroll.addView(content);
        tabsScroll = new HorizontalScrollView(this); tabsScroll.setHorizontalScrollBarEnabled(false); tabsScroll.setFillViewport(true);
        tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); tabs.setGravity(Gravity.CENTER);
        tabs.setPadding(0,dp(8),0,0); tabsScroll.addView(tabs,new FrameLayout.LayoutParams(-1,-1));
        stage.addView(tabsScroll,new FrameLayout.LayoutParams(-1,dp(64),Gravity.TOP));
        stage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,oright,ob) -> positionTabs());
        setContentView(root);
        showTab(tab);
    }
    private void positionTabs() {
        if (stage.getHeight() <= 0) return;
        float density=getResources().getDisplayMetrics().density;
        int top=Math.round(ClockHudView.frameGap(stage.getWidth(),stage.getHeight(),density)
                + ClockHudView.dialSize(stage.getWidth(),stage.getHeight(),density) + dp(4));
        top=Math.max(0,Math.min(top,stage.getHeight()-dp(64)));
        FrameLayout.LayoutParams params=(FrameLayout.LayoutParams) tabsScroll.getLayoutParams();
        if (params.topMargin != top) { params.topMargin=top; tabsScroll.setLayoutParams(params); }
        FrameLayout.LayoutParams panelParams=(FrameLayout.LayoutParams) panelScroll.getLayoutParams();
        int panelTop=Math.min(stage.getHeight(),top+dp(64));
        if (panelParams.topMargin != panelTop) { panelParams.topMargin=panelTop; panelScroll.setLayoutParams(panelParams); }
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
        tab = selected; tabs.removeAllViews(); content.removeAllViews(); alarmRows.clear(); lastAlarmSecond=-1; alarmEditor=null; soundButton=null; timerDisplay = null; stopwatchDisplay = null; worldList = null;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int index = i;
            ImageButton item = new ImageButton(this);
            item.setImageResource(TAB_ICONS[i]);
            item.setScaleType(ImageView.ScaleType.FIT_CENTER);
            item.setPadding(dp(12), dp(12), dp(12), dp(12));
            item.setBackground(bg(i == selected ? ui.selected : ui.control));
            item.setColorFilter(i == selected ? ui.selectedText : ui.ink);
            item.setContentDescription(TAB_NAMES[i]);
            item.setTooltipText(TAB_NAMES[i]);
            item.setSelected(i == selected);
            item.setOnClickListener(v -> showTab(tab == index ? -1 : index));
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            if (i > 0) itemParams.leftMargin = dp(5);
            tabs.addView(item, itemParams);
        }
        positionTabs();
        panelScroll.setVisibility(selected == -1 ? View.GONE : View.VISIBLE);
        if (selected == 0) showAlarms(); else if (selected == 1) showWorld(); else if (selected == 2) showTimer(); else if (selected == 3) showStopwatch();
        updateLive();
    }
    @Override public void onBackPressed() { if (tab == 0 && alarmEditor != null && alarmEditor.getVisibility() == View.VISIBLE) { alarmEditor.setVisibility(View.GONE); soundButton=null; } else if (tab != -1) showTab(-1); else super.onBackPressed(); }
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
        StringBuilder b = new StringBuilder(); for (int i : new int[]{1,2,3,4,5,6,0}) if ((mask & (1 << i)) != 0) { if (b.length() > 0) b.append(" · "); b.append(DAYS[i]); }
        return b.toString();
    }
    private void showAlarms() {
        LinearLayout heading=new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("ALARM",19,AMBER),new LinearLayout.LayoutParams(0,-2,1));
        ImageButton add=new ImageButton(this); add.setImageResource(R.drawable.ic_calendar_add); add.setColorFilter(ui.ink);
        add.setScaleType(ImageView.ScaleType.FIT_CENTER); add.setPadding(dp(16),dp(16),dp(16),dp(16));
        add.setBackground(bg(ui.control)); add.setContentDescription("Tambah alarm"); add.setTooltipText("Tambah alarm");
        add.setOnClickListener(v -> editAlarm(null)); heading.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));
        content.addView(heading);
        alarmEditor=new LinearLayout(this); alarmEditor.setOrientation(LinearLayout.VERTICAL); alarmEditor.setVisibility(View.GONE); content.addView(alarmEditor); addSpace(content, 12);
        JSONArray all = ClockAlarms.read(this);
        if (all.length() == 0) content.addView(text("Belum ada alarm. Tambahkan pengingat pertama.", 13, MUTED));
        for (int i = 0; i < all.length(); i++) {
            JSONObject a = all.optJSONObject(i); if (a == null) continue;
            LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(14),dp(12),dp(14),dp(12)); box.setBackground(bg(ui.card));
            LinearLayout alarmHead=new LinearLayout(this); alarmHead.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text(String.format(Locale.US, "%02d:%02d", a.optInt("hour"), a.optInt("minute")), 27, AMBER);
            alarmHead.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            String alarmSound=a.optString("sound",Settings.System.DEFAULT_ALARM_ALERT_URI.toString());
            ImageView soundIcon=new ImageView(this); soundIcon.setImageResource(R.drawable.ic_alarm_sound); soundIcon.setColorFilter(ui.ink); soundIcon.setAlpha(alarmSound.isEmpty() ? .4f : 1f); soundIcon.setContentDescription("Nada alarm: " + soundName(alarmSound));
            alarmHead.addView(soundIcon,new LinearLayout.LayoutParams(dp(20),dp(20)));
            ImageView vibrationIcon=new ImageView(this); vibrationIcon.setImageResource(R.drawable.ic_alarm_vibrate); vibrationIcon.setColorFilter(ui.ink); vibrationIcon.setAlpha(a.optBoolean("vibrate",true) ? 1f : .4f); vibrationIcon.setContentDescription(a.optBoolean("vibrate",true) ? "Getar aktif" : "Getar tidak aktif");
            LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(20),dp(20)); iconParams.leftMargin=dp(8); alarmHead.addView(vibrationIcon,iconParams); box.addView(alarmHead);
            box.addView(text(dayLabel(a.optInt("days")), 12, MUTED));
            TextView countdown=text("",13,MUTED); box.addView(countdown);
            Switch enabled = new Switch(this); enabled.setText("Aktif"); styleSwitch(enabled); enabled.setChecked(a.optBoolean("enabled"));
            AlarmRow alarmRow=new AlarmRow(a.optInt("id"),countdown,enabled); alarmRows.add(alarmRow);
            enabled.setOnCheckedChangeListener((v, checked) -> {
                if (alarmRow.updating) return;
                if (checked && !ensureExact()) { v.setChecked(false); return; }
                try {
                    a.put("enabled", checked); ClockAlarms.put(this, a);
                    if (checked) ClockAlarms.schedule(this, a);
                    else {
                        ClockAlarms.cancel(this, a.optInt("id"));
                        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1000000 + a.optInt("id"));
                    }
                } catch (JSONException ignored) { }
                lastAlarmSecond=-1; updateAlarmCountdowns();
            }); box.addView(enabled);
            LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.addView(button("Ubah", () -> editAlarm(a)), new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,-2,1); p.leftMargin=dp(8);
            actions.addView(button("Hapus", () -> ui.dialog(this).setMessage("Hapus alarm ini?").setPositiveButton("Hapus", (d,w) -> { ClockAlarms.remove(this, a.optInt("id")); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1000000 + a.optInt("id")); showTab(0); }).setNegativeButton("Batal",null).show()), p);
            box.addView(actions); content.addView(box); addSpace(content,10);
        }
    }
    private void updateAlarmCountdowns() {
        long now=System.currentTimeMillis();
        for (AlarmRow row : alarmRows) {
            JSONObject a=ClockAlarms.find(this,row.id);
            if (a == null) { row.countdown.setText("Alarm dihapus"); continue; }
            boolean enabled=a.optBoolean("enabled",false);
            if (row.enabled.isChecked()!=enabled) { row.updating=true; row.enabled.setChecked(enabled); row.updating=false; }
            long snooze=ClockAlarms.prefs(this).getLong("snooze_"+row.id,0);
            long when=enabled ? ClockAlarms.nextTime(a,now) : 0;
            if (snooze>now && (when==0 || snooze<when)) when=snooze;
            row.countdown.setText(when==0 ? "Tidak aktif" : (!ClockAlarms.canSchedule(this) ? "Izin alarm diperlukan" : ClockTime.countdown(when,now)));
        }
    }
    private ImageButton alarmOption(int icon, String description, Runnable action) {
        ImageButton v=new ImageButton(this); v.setImageResource(icon); v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setPadding(dp(12),dp(12),dp(12),dp(12)); v.setBackground(bg(ui.control)); v.setColorFilter(ui.ink);
        v.setContentDescription(description); v.setTooltipText(description); v.setOnClickListener(w -> action.run()); return v;
    }
    private void updateSoundDescription() {
        if (soundButton==null) return;
        String description="Nada alarm: " + soundName(editingSound);
        soundButton.setContentDescription(description); soundButton.setTooltipText(description);
        soundButton.setAlpha(editingSound.isEmpty() ? .55f : 1f);
    }
    private void editAlarm(JSONObject existing) {
        if (alarmEditor==null) return;
        final int[] hour = {existing == null ? 7 : existing.optInt("hour",7)};
        final int[] minute = {existing == null ? 0 : existing.optInt("minute",0)};
        final int[] days = {existing == null ? 0 : existing.optInt("days",0)};
        final boolean[] vibrate = {existing == null || existing.optBoolean("vibrate",true)};
        editingSound = existing == null ? Settings.System.DEFAULT_ALARM_ALERT_URI.toString() : existing.optString("sound", Settings.System.DEFAULT_ALARM_ALERT_URI.toString());
        alarmEditor.removeAllViews(); alarmEditor.setPadding(dp(12),dp(12),dp(12),dp(12)); alarmEditor.setBackground(bg(ui.event)); alarmEditor.setVisibility(View.VISIBLE);
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView time = button("", () -> {});
        Runnable updateTime = () -> time.setText(String.format(Locale.US, "%02d:%02d", hour[0], minute[0])); updateTime.run();
        time.setOnClickListener(v -> new TimePickerDialog(this, ui.dark ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert, (picker,h,m) -> { hour[0]=h; minute[0]=m; updateTime.run(); }, hour[0],minute[0],true).show());
        row.addView(time,new LinearLayout.LayoutParams(0,dp(48),1));
        soundButton=alarmOption(R.drawable.ic_alarm_sound,"Nada alarm",() -> pickSound());
        LinearLayout.LayoutParams soundParams=new LinearLayout.LayoutParams(dp(48),dp(48)); soundParams.leftMargin=dp(4); row.addView(soundButton,soundParams); updateSoundDescription();
        ImageButton vibration=alarmOption(R.drawable.ic_alarm_vibrate,"Getar",() -> {});
        Runnable updateVibrate=() -> { vibration.setSelected(vibrate[0]); vibration.setBackground(bg(vibrate[0] ? ui.selected : ui.control)); vibration.setColorFilter(vibrate[0] ? ui.selectedText : ui.ink); vibration.setContentDescription("Getar " + (vibrate[0] ? "aktif" : "tidak aktif")); vibration.setTooltipText("Getar " + (vibrate[0] ? "aktif" : "tidak aktif")); };
        vibration.setOnClickListener(v -> { vibrate[0]=!vibrate[0]; updateVibrate.run(); }); updateVibrate.run();
        LinearLayout.LayoutParams vibParams=new LinearLayout.LayoutParams(dp(48),dp(48)); vibParams.leftMargin=dp(4); row.addView(vibration,vibParams); alarmEditor.addView(row); addSpace(alarmEditor,8);
        HorizontalScrollView daysScroll=new HorizontalScrollView(this); daysScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout dayRow=new LinearLayout(this); dayRow.setOrientation(LinearLayout.HORIZONTAL); dayRow.setGravity(Gravity.CENTER);
        final int[] order={1,2,3,4,5,6,0};
        for (int day : order) {
            TextView chip=text(DAYS[day],11,AMBER); chip.setGravity(Gravity.CENTER); chip.setMinHeight(dp(48));
            Runnable refresh=() -> { boolean selected=(days[0] & (1<<day))!=0; chip.setBackground(bg(selected ? ui.selected : ui.control)); chip.setTextColor(selected ? ui.selectedText : ui.ink); chip.setSelected(selected); chip.setContentDescription(DAYS[day] + (selected ? ", dipilih" : ", tidak dipilih")); };
            chip.setOnClickListener(v -> { days[0]^=1<<day; refresh.run(); }); refresh.run();
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(dp(48),dp(48)); if (day!=1) params.leftMargin=dp(3); dayRow.addView(chip,params);
        }
        daysScroll.addView(dayRow); alarmEditor.addView(daysScroll); addSpace(alarmEditor,8);
        LinearLayout actions=new LinearLayout(this);
        actions.addView(button("Batal",() -> { alarmEditor.setVisibility(View.GONE); soundButton=null; }),new LinearLayout.LayoutParams(0,dp(48),1));
        LinearLayout.LayoutParams saveParams=new LinearLayout.LayoutParams(0,dp(48),1); saveParams.leftMargin=dp(8);
        actions.addView(button("Simpan",() -> {
            if (!ensureExact()) return;
            try {
                JSONObject a = existing == null ? new JSONObject() : existing;
                if (existing == null) a.put("id", ClockAlarms.nextId(this));
                ClockAlarms.cancel(this,a.optInt("id"));
                ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1000000 + a.optInt("id"));
                a.put("hour",hour[0]); a.put("minute",minute[0]); a.put("days",days[0]); a.put("sound",editingSound); a.put("vibrate",vibrate[0]); a.put("enabled",true);
                ClockAlarms.put(this,a);
                if (!ClockAlarms.schedule(this,a)) Toast.makeText(this,"Alarm belum terjadwal. Periksa izin alarm.",Toast.LENGTH_LONG).show();
                showTab(0);
            } catch (JSONException ignored) { }
        }),saveParams); alarmEditor.addView(actions);
        LinearLayout editor=alarmEditor; panelScroll.post(() -> { if (alarmEditor==editor) panelScroll.smoothScrollTo(0,editor.getTop()); });
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
            editingSound = picked == null ? "" : picked.toString(); updateSoundDescription();
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
        timerDisplay=text("00:00:00",32,AMBER); timerDisplay.setGravity(Gravity.CENTER); timerDisplay.setBackground(bg(ui.card)); timerDisplay.setPadding(dp(8),dp(12),dp(8),dp(12)); content.addView(timerDisplay); addSpace(content,12);
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
        stopwatchDisplay=text(formatElapsed(elapsed()),27,AMBER);stopwatchDisplay.setGravity(Gravity.CENTER);stopwatchDisplay.setBackground(bg(ui.card));stopwatchDisplay.setPadding(dp(8),dp(12),dp(8),dp(12));content.addView(stopwatchDisplay);addSpace(content,12);
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
        long second=System.currentTimeMillis()/1000;
        if (hud != null && second != lastHudSecond) { lastHudSecond=second; hud.setTime(Calendar.getInstance()); }
        if(worldList!=null&&second!=lastWorldSecond){lastWorldSecond=second;updateWorld();}
        if(tab==0&&second!=lastAlarmSecond){lastAlarmSecond=second;updateAlarmCountdowns();}
    }
    @Override protected void onResume() { super.onResume(); AppUi current=new AppUi(this); if (ui.dark != current.dark || !ui.choice.equals(current.choice)) { recreate(); return; } if(awaitingExactPermission){awaitingExactPermission=false;if(ClockAlarms.canSchedule(this))requestAlerts();} ClockAlarms.restore(this);lastHudSecond=-1;handler.removeCallbacks(tick);tick.run(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
}
