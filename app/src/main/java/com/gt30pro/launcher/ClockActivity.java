package com.gt30pro.launcher;

import android.Manifest;
import android.app.NotificationManager;
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
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
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
    private NumberPicker timerHours, timerMinutes, timerSeconds;
    private String[] timerDraft = {"", "", ""};
    private static final String[] TAB_NAMES = {"Alarm", "Jam Dunia", "Timer", "Stopwatch"};
    private static final int[] TAB_ICONS = {R.drawable.ic_clock_alarm, R.drawable.ic_clock_world,
            R.drawable.ic_clock_timer, R.drawable.ic_clock_stopwatch};
    private static final String[] CITIES = {"Jakarta", "Makassar", "Jayapura", "Singapore", "Tokyo", "Seoul", "Dubai", "London", "Paris", "New York", "Los Angeles", "Sydney"};
    // Keep the original city indices for existing saved selections. Overseas order is
    // approximate great-circle distance from Jakarta; Indonesian cities come first.
    private static final int MAX_WORLD_CITIES = 5;
    private static final int[] CITY_ORDER = {0,1,2,3,5,11,4,6,8,7,10,9};
    private static final String[] ZONES = {"Asia/Jakarta", "Asia/Makassar", "Asia/Jayapura", "Asia/Singapore", "Asia/Tokyo", "Asia/Seoul", "Asia/Dubai", "Europe/London", "Europe/Paris", "America/New_York", "America/Los_Angeles", "Australia/Sydney"};
    private static final String[] DAYS = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Typeface doto;
    private LinearLayout content, tabs, featureHeader;
    private HorizontalScrollView tabsScroll;
    private FrameLayout stage;
    private ClockHudView hud;
    private View tabIndicator;
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
        tabIndicator=new View(this); tabIndicator.setBackground(ui.iconGlass(this,true)); tabIndicator.setVisibility(View.INVISIBLE);
        stage.addView(tabIndicator,new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.LEFT));
        stage.addView(tabsScroll,new FrameLayout.LayoutParams(-1,dp(64),Gravity.TOP));
        tabsScroll.setOnScrollChangeListener((v,x,y,oldX,oldY) -> positionTabIndicator(false));
        featureHeader=new LinearLayout(this); featureHeader.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams headerParams=new FrameLayout.LayoutParams(-1,dp(48),Gravity.TOP); headerParams.leftMargin=dp(16); headerParams.rightMargin=dp(16);
        stage.addView(featureHeader,headerParams);
        stage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,oright,ob) -> { positionTabs(); tabs.post(() -> positionTabIndicator(false)); });
        setContentView(root);
        showTab(tab);
        String action=getIntent().getStringExtra("voice_action");
        if (action!=null) { getIntent().removeExtra("voice_action"); handleVoiceAction(action); }
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        String action=intent.getStringExtra("voice_action");
        if(action!=null) { intent.removeExtra("voice_action"); handleVoiceAction(action); }
    }
    @Override protected boolean handleVoiceAction(String action) {
        if ("alarm".equals(action)) { showTab(0); return true; }
        if ("world".equals(action)) { showTab(1); return true; }
        if ("timer".equals(action)) { showTab(2); return true; }
        if ("stopwatch".equals(action)) { showTab(3); return true; }
        if ("add_alarm".equals(action)) { showTab(0); editAlarm(null); return true; }
        if ("start_stopwatch".equals(action)) { if(!swRunning){swStarted=SystemClock.elapsedRealtime();swRunning=true;saveStopwatch();} showTab(3); return true; }
        if ("pause_stopwatch".equals(action)) { if(swRunning){swAccumulated=elapsed();swRunning=false;saveStopwatch();} showTab(3); return true; }
        if ("lap".equals(action)) { if(swRunning){laps.put(elapsed());saveStopwatch();} showTab(3); return true; }
        return false;
    }
    private void positionTabs() {
        if (stage.getHeight() <= 0) return;
        float density=getResources().getDisplayMetrics().density;
        int top=Math.round(ClockHudView.frameGap(stage.getWidth(),stage.getHeight(),density)
                + ClockHudView.dialSize(stage.getWidth(),stage.getHeight(),density,(tab >= 0)) + dp(4));
        top=Math.max(0,Math.min(top,stage.getHeight()-dp(64)));
        FrameLayout.LayoutParams params=(FrameLayout.LayoutParams) tabsScroll.getLayoutParams();
        if (params.topMargin != top) { params.topMargin=top; tabsScroll.setLayoutParams(params); }
        FrameLayout.LayoutParams headerParams=(FrameLayout.LayoutParams) featureHeader.getLayoutParams();
        int headerTop=Math.min(stage.getHeight(),top+dp(64));
        if (headerParams.topMargin != headerTop) { headerParams.topMargin=headerTop; featureHeader.setLayoutParams(headerParams); }
        FrameLayout.LayoutParams panelParams=(FrameLayout.LayoutParams) panelScroll.getLayoutParams();
        int panelTop=Math.min(stage.getHeight(),headerTop+(tab >= 1 ? dp(48) : 0));
        if (panelParams.topMargin != panelTop) { panelParams.topMargin=panelTop; panelScroll.setLayoutParams(panelParams); }
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        saveTimerDraft(); state.putInt("clock_tab",tab); state.putStringArray("timer_draft",timerDraft); super.onSaveInstanceState(state);
    }
    private void saveTimerDraft() {
        if (timerHours != null) timerDraft = new String[]{String.valueOf(timerHours.getValue()), String.valueOf(timerMinutes.getValue()), String.valueOf(timerSeconds.getValue())};
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
        hud.setEditorOpen(selected >= 0);
        tab = selected; tabs.removeAllViews(); content.removeAllViews(); alarmRows.clear(); lastAlarmSecond=-1; alarmEditor=null; soundButton=null; timerDisplay = null; stopwatchDisplay = null; worldList = null;
        for (int i = 0; i < TAB_NAMES.length; i++) {
            final int index = i;
            ImageButton item = new ImageButton(this);
            item.setImageResource(TAB_ICONS[i]);
            item.setScaleType(ImageView.ScaleType.FIT_CENTER);
            item.setPadding(dp(12), dp(12), dp(12), dp(12));
            item.setBackground(ui.iconGlass(this,false));
            item.setColorFilter(ui.ink);
            item.setContentDescription(TAB_NAMES[i]);
            item.setTooltipText(TAB_NAMES[i]);
            item.setSelected(i == selected);
            item.setOnClickListener(v -> showTab(tab == index ? -1 : index));
            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(dp(48), dp(48));
            if (i > 0) itemParams.leftMargin = dp(5);
            tabs.addView(item, itemParams);
        }
        featureHeader.removeAllViews(); featureHeader.setVisibility(selected >= 1 ? View.VISIBLE : View.GONE);
        positionTabs();
        panelScroll.setVisibility(selected == -1 ? View.GONE : View.VISIBLE);
        if (selected == 0) showAlarms(); else if (selected == 1) showWorld(); else if (selected == 2) showTimer(); else if (selected == 3) showStopwatch();
        if(selected<0) { tabIndicator.animate().cancel(); tabIndicator.setVisibility(View.INVISIBLE); }
        else tabs.post(() -> { if(tab==selected) positionTabIndicator(true); });
        updateLive();
    }
    private void positionTabIndicator(boolean animate) {
        if(tabIndicator==null || tab<0 || tab>=tabs.getChildCount()) return;
        View target=tabs.getChildAt(tab); int[] childPos=new int[2],stagePos=new int[2];
        target.getLocationInWindow(childPos); stage.getLocationInWindow(stagePos);
        float x=childPos[0]-stagePos[0], y=childPos[1]-stagePos[1];
        boolean visible=tabIndicator.getVisibility()==View.VISIBLE;
        tabIndicator.animate().cancel(); tabIndicator.setVisibility(View.VISIBLE);
        if(animate && visible && android.animation.ValueAnimator.areAnimatorsEnabled()) tabIndicator.animate().x(x).y(y).setDuration(240).start();
        else { tabIndicator.setX(x); tabIndicator.setY(y); }
    }
    @Override public void onBackPressed() { if (tab == 0 && alarmEditor != null && alarmEditor.getVisibility() == View.VISIBLE) { closeAlarmEditor(); } else if (tab != -1) showTab(-1); else super.onBackPressed(); }
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
        if (mask == 0) return ""; if (mask == 127) return "Setiap hari";
        StringBuilder b = new StringBuilder(); for (int i : new int[]{1,2,3,4,5,6,0}) if ((mask & (1 << i)) != 0) { if (b.length() > 0) b.append(" · "); b.append(DAYS[i]); }
        return b.toString();
    }
    private void showAlarms() {
        LinearLayout heading=new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("ALARM",19,AMBER),new LinearLayout.LayoutParams(0,-2,1));
        ImageButton add=new ImageButton(this); add.setImageResource(R.drawable.ic_calendar_add); add.setColorFilter(ui.ink);
        add.setScaleType(ImageView.ScaleType.FIT_CENTER); add.setPadding(dp(18),dp(18),dp(18),dp(18));
        add.setBackground(ui.iconGlass(this,false)); add.setContentDescription("Tambah alarm"); add.setTooltipText("Tambah alarm");
        add.setOnClickListener(v -> editAlarm(null)); heading.addView(add,new LinearLayout.LayoutParams(dp(48),dp(48)));
        content.addView(heading);
        alarmEditor=new LinearLayout(this); alarmEditor.setOrientation(LinearLayout.VERTICAL); alarmEditor.setVisibility(View.GONE); content.addView(alarmEditor); addSpace(content, 12);
        JSONArray all = ClockAlarms.read(this);
        if (all.length() == 0) content.addView(text("Belum ada alarm. Tambahkan pengingat pertama.", 13, MUTED));
        for (int i = 0; i < all.length(); i++) {
            JSONObject a = all.optJSONObject(i); if (a == null) continue;
            LinearLayout box = new LinearLayout(this); box.setGravity(Gravity.CENTER_VERTICAL); box.setPadding(dp(12),dp(8),dp(8),dp(8)); box.setBackground(bg(ui.card));
            LinearLayout details=new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL);
            TextView label = text(String.format(Locale.US, "%02d:%02d", a.optInt("hour"), a.optInt("minute")), 27, AMBER);
            label.setContentDescription("Ubah alarm " + label.getText()); label.setOnClickListener(v -> editAlarm(a));
            LinearLayout timeRow=new LinearLayout(this); timeRow.setGravity(Gravity.CENTER_VERTICAL); timeRow.addView(label);

            String alarmSound=a.optString("sound",Settings.System.DEFAULT_ALARM_ALERT_URI.toString());
            ImageView soundIcon=new ImageView(this); soundIcon.setImageResource(R.drawable.ic_alarm_sound); soundIcon.setColorFilter(ui.muted); soundIcon.setAlpha(alarmSound.isEmpty() ? .4f : 1f); soundIcon.setContentDescription("Nada alarm: " + soundName(alarmSound));
            LinearLayout.LayoutParams soundInfoParams=new LinearLayout.LayoutParams(dp(16),dp(16)); soundInfoParams.leftMargin=dp(8); timeRow.addView(soundIcon,soundInfoParams);
            ImageView vibrationIcon=new ImageView(this); vibrationIcon.setImageResource(R.drawable.ic_alarm_vibrate); vibrationIcon.setColorFilter(ui.muted); vibrationIcon.setAlpha(a.optBoolean("vibrate",true) ? 1f : .4f); vibrationIcon.setContentDescription(a.optBoolean("vibrate",true) ? "Getar aktif" : "Getar tidak aktif");
            LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(16),dp(16)); iconParams.leftMargin=dp(4); timeRow.addView(vibrationIcon,iconParams); details.addView(timeRow);
            TextView countdown=text("",13,MUTED); details.addView(countdown);
            if (a.optInt("days") != 0) details.addView(text(dayLabel(a.optInt("days")), 12, MUTED));
            box.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            Switch enabled = new Switch(this); styleSwitch(enabled);
            int[][] switchStates={new int[]{android.R.attr.state_checked},new int[]{}};
            enabled.setThumbTintList(new ColorStateList(switchStates,new int[]{ui.selectedText,ui.dark ? ui.card : ui.ink}));
            enabled.setTrackTintList(new ColorStateList(switchStates,new int[]{ui.selected,ui.border}));
            enabled.setContentDescription("Alarm " + label.getText()); enabled.setChecked(a.optBoolean("enabled"));
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
            ImageButton remove=alarmOption(R.drawable.ic_alarm_delete,"Hapus alarm " + label.getText(),() -> deleteAlarm(a));
            LinearLayout.LayoutParams deleteParams=new LinearLayout.LayoutParams(dp(48),dp(48)); deleteParams.leftMargin=dp(4); box.addView(remove,deleteParams);
            content.addView(box); addSpace(content,10);
        }
    }
    private void deleteAlarm(JSONObject alarm) {
        ui.dialog(this).setMessage("Hapus alarm ini?").setPositiveButton("Hapus", (d,w) -> {
            ClockAlarms.remove(this,alarm.optInt("id"));
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1000000+alarm.optInt("id"));
            showTab(0);
        }).setNegativeButton("Batal",null).show();
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
            row.countdown.setText(when==0 ? "—" : (!ClockAlarms.canSchedule(this) ? "Izin alarm diperlukan" : ClockTime.countdown(when,now)));
        }
    }
    private ImageButton alarmOption(int icon, String description, Runnable action) {
        ImageButton v=new ImageButton(this); v.setImageResource(icon); v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setPadding(dp(12),dp(12),dp(12),dp(12)); v.setBackground(ui.iconGlass(this,false)); v.setColorFilter(ui.ink);
        v.setContentDescription(description); v.setTooltipText(description); v.setOnClickListener(w -> action.run()); return v;
    }
    private void updateSoundDescription() {
        if (soundButton==null) return;
        String description="Nada alarm: " + soundName(editingSound);
        soundButton.setContentDescription(description); soundButton.setTooltipText(description);
        soundButton.setAlpha(editingSound.isEmpty() ? .55f : 1f);
    }
    private NumberPicker alarmWheel(int maximum, int value, String description) {
        NumberPicker picker=new NumberPicker(this); picker.setMinValue(0); picker.setMaxValue(maximum); picker.setWrapSelectorWheel(true);
        picker.setFormatter(n -> String.format(Locale.US,"%02d",n)); picker.setValue(value);
        picker.setContentDescription(description); picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        if (Build.VERSION.SDK_INT >= 29) picker.setTextColor(ui.ink);
        picker.setOnTouchListener((v,event) -> {
            if (event.getActionMasked()==MotionEvent.ACTION_DOWN) v.getParent().requestDisallowInterceptTouchEvent(true);
            else if (event.getActionMasked()==MotionEvent.ACTION_UP || event.getActionMasked()==MotionEvent.ACTION_CANCEL) v.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
        });
        return picker;
    }
    private void editAlarm(JSONObject existing) {
        if (alarmEditor==null) return;
        final int[] hour = {existing == null ? 7 : existing.optInt("hour",7)};
        final int[] minute = {existing == null ? 0 : existing.optInt("minute",0)};
        final int[] days = {existing == null ? 0 : existing.optInt("days",0)};
        final boolean[] vibrate = {existing == null || existing.optBoolean("vibrate",true)};
        editingSound = existing == null ? Settings.System.DEFAULT_ALARM_ALERT_URI.toString() : existing.optString("sound", Settings.System.DEFAULT_ALARM_ALERT_URI.toString());
        alarmEditor.removeAllViews(); alarmEditor.setPadding(dp(8),dp(8),dp(8),dp(8)); alarmEditor.setBackground(bg(ui.event)); alarmEditor.setVisibility(View.VISIBLE);
        hud.setEditorOpen(true); positionTabs(); panelScroll.scrollTo(0,0);
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView timeLabel=text("Waktu",13,AMBER); row.addView(timeLabel,new LinearLayout.LayoutParams(0,dp(48),1));
        soundButton=alarmOption(R.drawable.ic_alarm_sound,"Nada alarm",() -> pickSound());
        LinearLayout.LayoutParams soundParams=new LinearLayout.LayoutParams(dp(48),dp(48)); soundParams.leftMargin=dp(4); row.addView(soundButton,soundParams); updateSoundDescription();
        ImageButton vibration=alarmOption(R.drawable.ic_alarm_vibrate,"Getar",() -> {});
        Runnable updateVibrate=() -> { vibration.setSelected(vibrate[0]); vibration.setBackground(bg(vibrate[0] ? ui.selected : ui.control)); vibration.setColorFilter(vibrate[0] ? ui.selectedText : ui.ink); vibration.setContentDescription("Getar " + (vibrate[0] ? "aktif" : "tidak aktif")); vibration.setTooltipText("Getar " + (vibrate[0] ? "aktif" : "tidak aktif")); };
        vibration.setOnClickListener(v -> { vibrate[0]=!vibrate[0]; updateVibrate.run(); }); updateVibrate.run();
        LinearLayout.LayoutParams vibParams=new LinearLayout.LayoutParams(dp(48),dp(48)); vibParams.leftMargin=dp(4); row.addView(vibration,vibParams); alarmEditor.addView(row); addSpace(alarmEditor,8);
        FrameLayout wheelFrame=new FrameLayout(this);
        View selectionBand=new View(this); selectionBand.setBackground(bg(ui.control));
        wheelFrame.addView(selectionBand,new FrameLayout.LayoutParams(-1,dp(48),Gravity.CENTER));
        LinearLayout wheels=new LinearLayout(this); wheels.setGravity(Gravity.CENTER);
        NumberPicker hours=alarmWheel(23,hour[0],"Jam"), minutes=alarmWheel(59,minute[0],"Menit");
        wheels.addView(hours,new LinearLayout.LayoutParams(dp(80),dp(120)));
        TextView separator=text(":",24,AMBER); separator.setGravity(Gravity.CENTER); wheels.addView(separator,new LinearLayout.LayoutParams(dp(24),dp(48)));
        wheels.addView(minutes,new LinearLayout.LayoutParams(dp(80),dp(120)));
        wheelFrame.addView(wheels,new FrameLayout.LayoutParams(-1,dp(120),Gravity.CENTER)); alarmEditor.addView(wheelFrame); addSpace(alarmEditor,8);
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
        LinearLayout actions=new LinearLayout(this); actions.setGravity(Gravity.END);
        actions.addView(alarmOption(R.drawable.ic_calendar_cancel,"Batal",() -> closeAlarmEditor()),new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout.LayoutParams saveParams=new LinearLayout.LayoutParams(dp(48),dp(48)); saveParams.leftMargin=dp(8);
        actions.addView(alarmOption(R.drawable.ic_calendar_save,"Simpan alarm",() -> {
            boolean enabled=existing == null || existing.optBoolean("enabled",false);
            if (enabled && !ensureExact()) return;
            try {
                JSONObject a = existing == null ? new JSONObject() : existing;
                if (existing == null) a.put("id", ClockAlarms.nextId(this));
                ClockAlarms.cancel(this,a.optInt("id"));
                ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1000000 + a.optInt("id"));
                a.put("hour",hours.getValue()); a.put("minute",minutes.getValue()); a.put("days",days[0]); a.put("sound",editingSound); a.put("vibrate",vibrate[0]); a.put("enabled",enabled);
                ClockAlarms.put(this,a);
                if (enabled && !ClockAlarms.schedule(this,a)) Toast.makeText(this,"Alarm belum terjadwal. Periksa izin alarm.",Toast.LENGTH_LONG).show();
                showTab(0);
            } catch (JSONException ignored) { }
        }),saveParams);
        if (existing != null) {
            LinearLayout.LayoutParams deleteParams=new LinearLayout.LayoutParams(dp(48),dp(48)); deleteParams.leftMargin=dp(8);
            actions.addView(alarmOption(R.drawable.ic_alarm_delete,"Hapus alarm",() -> deleteAlarm(existing)),deleteParams);
        }
        alarmEditor.addView(actions);
    }
    private void closeAlarmEditor() {
        if (alarmEditor != null) alarmEditor.setVisibility(View.GONE);
        soundButton=null; hud.setEditorOpen(tab >= 0); positionTabs();
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
        return WorldCities.limit(selected,CITY_ORDER,MAX_WORLD_CITIES);
    }
    private void setFeatureTitle(String title) {
        TextView label=text(title,19,AMBER); label.setSingleLine(true); label.setEllipsize(TextUtils.TruncateAt.END);
        featureHeader.addView(label,new LinearLayout.LayoutParams(0,dp(48),1)); label.setGravity(Gravity.CENTER_VERTICAL);
    }
    private void addFeatureAction(ImageButton button) {
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(dp(48),dp(48)); params.leftMargin=dp(4);
        featureHeader.addView(button,params);
    }
    private void showWorld() {
        setFeatureTitle("JAM DUNIA");
        ImageButton choose=alarmOption(R.drawable.ic_calendar_add,"Pilih kota",() -> {
            boolean[] chosen=selectedCities();
            ScrollView chooser=new ScrollView(this);
            LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(16),0,dp(16),0);
            for (int index : CITY_ORDER) {
                LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setMinimumHeight(dp(56));
                TextView city=text(CITIES[index],15,AMBER); city.setGravity(Gravity.CENTER_VERTICAL); city.setPadding(0,dp(8),dp(12),dp(8));
                row.addView(city,new LinearLayout.LayoutParams(0,-1,1));
                Switch toggle=new Switch(this); styleSwitch(toggle);
                int[][] states={new int[]{android.R.attr.state_checked},new int[]{}};
                toggle.setThumbTintList(new ColorStateList(states,new int[]{ui.selectedText,ui.dark ? ui.card : ui.ink}));
                toggle.setTrackTintList(new ColorStateList(states,new int[]{ui.selected,ui.border}));
                toggle.setContentDescription(CITIES[index]); toggle.setChecked(chosen[index]);
                toggle.setOnCheckedChangeListener((v,on) -> {
                    if (on && WorldCities.count(chosen)>=MAX_WORLD_CITIES) {
                        v.setChecked(false); Toast.makeText(this,"Maksimal 5 kota. Nonaktifkan kota lain terlebih dahulu.",Toast.LENGTH_SHORT).show(); return;
                    }
                    chosen[index]=on;
                }); row.addView(toggle,new LinearLayout.LayoutParams(-2,dp(48)));
                list.addView(row,new LinearLayout.LayoutParams(-1,dp(56)));
            }
            chooser.addView(list);
            ui.dialog(this).setTitle("Pilih kota").setView(chooser).setPositiveButton("Simpan",(d,w)->{
                StringBuilder b=new StringBuilder(); for(int i=0;i<chosen.length;i++) if(chosen[i]) { if(b.length()>0)b.append(',');b.append(i); }
                ClockAlarms.prefs(this).edit().putString("cities",b.toString()).apply(); showTab(1);
            }).setNegativeButton("Batal",null).show();
        }); choose.setPadding(dp(16),dp(16),dp(16),dp(16)); addFeatureAction(choose);
        worldList = new LinearLayout(this); worldList.setOrientation(LinearLayout.VERTICAL); content.addView(worldList); lastWorldSecond=-1; updateWorld();
    }
    private void updateWorld() {
        if (worldList == null) return; worldList.removeAllViews(); boolean[] chosen=selectedCities(); Date now=new Date(); boolean any=false;
        for(int i : CITY_ORDER) if(chosen[i]) {
            any=true; TimeZone zone=TimeZone.getTimeZone(ZONES[i]);
            SimpleDateFormat timeFormat=new SimpleDateFormat("HH:mm",new Locale("id","ID")); timeFormat.setTimeZone(zone);
            SimpleDateFormat dateFormat=new SimpleDateFormat("EEE, d MMM",new Locale("id","ID")); dateFormat.setTimeZone(zone);
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(12),dp(6),dp(12),dp(6)); row.setBackground(bg(ui.card));
            TextView city=text(CITIES[i],16,AMBER); city.setSingleLine(true); city.setEllipsize(TextUtils.TruncateAt.END); city.setPadding(0,0,dp(8),0);
            row.addView(city,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout right=new LinearLayout(this); right.setOrientation(LinearLayout.VERTICAL); right.setGravity(Gravity.END);
            TextView time=text(timeFormat.format(now),17,AMBER); time.setGravity(Gravity.END); right.addView(time);
            TextView date=text(dateFormat.format(now),11,MUTED); date.setGravity(Gravity.END); right.addView(date);
            row.addView(right,new LinearLayout.LayoutParams(-2,-2)); worldList.addView(row,new LinearLayout.LayoutParams(-1,dp(56))); addSpace(worldList,6);
        }
        if(!any) worldList.addView(text("Pilih kota untuk ditampilkan.",13,MUTED));
    }
    private int draftValue(String value,int max) {
        try { return Math.max(0,Math.min(max,Integer.parseInt(value))); } catch (Exception ignored) { return 0; }
    }
    private void showTimer() {
        setFeatureTitle("TIMER");
        timerDisplay=text("00:00:00",32,AMBER); timerDisplay.setGravity(Gravity.CENTER); timerDisplay.setBackground(bg(ui.card)); timerDisplay.setPadding(dp(8),dp(12),dp(8),dp(12)); content.addView(timerDisplay); addSpace(content,8);
        FrameLayout frame=new FrameLayout(this); View band=new View(this); band.setBackground(bg(ui.control)); frame.addView(band,new FrameLayout.LayoutParams(-1,dp(48),Gravity.CENTER));
        LinearLayout wheels=new LinearLayout(this); wheels.setGravity(Gravity.CENTER);
        timerHours=alarmWheel(999,draftValue(timerDraft[0],999),"Jam timer"); timerMinutes=alarmWheel(59,draftValue(timerDraft[1],59),"Menit timer"); timerSeconds=alarmWheel(59,draftValue(timerDraft[2],59),"Detik timer");
        wheels.addView(timerHours,new LinearLayout.LayoutParams(0,dp(120),1));
        wheels.addView(timerMinutes,new LinearLayout.LayoutParams(0,dp(120),1));
        wheels.addView(timerSeconds,new LinearLayout.LayoutParams(0,dp(120),1));
        frame.addView(wheels,new FrameLayout.LayoutParams(-1,dp(120),Gravity.CENTER)); content.addView(frame); addSpace(content,8);
        addFeatureAction(alarmOption(R.drawable.ic_clock_action_play,"Mulai timer",()->{
            long hours=timerHours.getValue(), minutes=timerMinutes.getValue(), seconds=timerSeconds.getValue();
            if(hours==0&&minutes==0&&seconds==0) { Toast.makeText(this,"Pilih durasi timer",Toast.LENGTH_LONG).show();return; }
            if(!ensureExact())return; long end=System.currentTimeMillis()+(hours*3600+minutes*60+seconds)*1000;
            if(ClockAlarms.scheduleAt(this,ClockAlarms.TIMER_FIRE,0,end)){ClockAlarms.prefs(this).edit().putLong("timer_end",end).apply(); updateLive();}
            else Toast.makeText(this,"Timer belum terjadwal. Periksa izin alarm.",Toast.LENGTH_LONG).show();
        }));
        addFeatureAction(alarmOption(R.drawable.ic_clock_action_stop,"Batalkan timer",()->{ClockAlarms.cancelTimer(this); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(2000000); updateLive();}));
        addSpace(content,10); content.addView(text("Timer tetap berjalan saat halaman ditutup.",12,MUTED));
    }
    private long elapsed() { return swAccumulated + (swRunning ? SystemClock.elapsedRealtime()-swStarted : 0); }
    private String formatElapsed(long ms) { return String.format(Locale.US,"%02d:%02d:%02d.%03d",ms/3600000,(ms/60000)%60,(ms/1000)%60,ms%1000); }
    private void showStopwatch() {
        setFeatureTitle("STOPWATCH");
        stopwatchDisplay=text(formatElapsed(elapsed()),27,AMBER);stopwatchDisplay.setGravity(Gravity.CENTER);stopwatchDisplay.setBackground(bg(ui.card));stopwatchDisplay.setPadding(dp(8),dp(12),dp(8),dp(12));content.addView(stopwatchDisplay);addSpace(content,12);
        addFeatureAction(alarmOption(swRunning ? R.drawable.ic_clock_action_pause : R.drawable.ic_clock_action_play,swRunning ? "Jeda stopwatch" : "Mulai stopwatch",()->{
            if(swRunning){swAccumulated=elapsed();swRunning=false;}else{swStarted=SystemClock.elapsedRealtime();swRunning=true;} saveStopwatch();showTab(3);
        }));
        addFeatureAction(alarmOption(R.drawable.ic_clock_action_lap,"Catat lap",()->{ if(!swRunning)return;laps.put(elapsed());saveStopwatch();showTab(3); }));
        addFeatureAction(alarmOption(R.drawable.ic_clock_action_reset,"Reset stopwatch",()->{swRunning=false;swAccumulated=0;swStarted=0;laps=new JSONArray();saveStopwatch();showTab(3);}));
        for(int i=0;i<laps.length();i++) {
            long total=laps.optLong(i); long prior=i>0?laps.optLong(i-1):0;
            LinearLayout lap=new LinearLayout(this); lap.setOrientation(LinearLayout.VERTICAL); lap.setPadding(dp(8),dp(6),dp(8),dp(6));
            lap.addView(lapLine("Lap " + (i+1),formatElapsed(total-prior)));
            lap.addView(lapLine("Total",formatElapsed(total))); content.addView(lap);
        }
    }
    private LinearLayout lapLine(String label, String value) {
        LinearLayout line=new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL);
        TextView caption=text(label,13,MUTED); line.addView(caption,new LinearLayout.LayoutParams(0,-2,1));
        TextView duration=text(value,13,MUTED); duration.setGravity(Gravity.END); line.addView(duration,new LinearLayout.LayoutParams(-2,-2));
        return line;
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
