package com.gt30pro.launcher;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import android.os.Handler;
import android.os.Looper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** Kalender lokal; data tidak disinkronkan dan tidak menjadwalkan pengingat. */
public class CalendarActivity extends SwipeBackActivity {
    @Override protected boolean supportsBottomSwipe() { return true; }
    private int ink, muted, weekend, observance, canvasColor, cardColor, controlColor, dayColor, selectionColor, selectedText, selectedWeekend, borderColor, eventColor, todayColor;
    private boolean dark;
    private String themeChoice;
    private ScrollView scroll;
    private static final String[] MONTHS = {"Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
    private static final String[] WEEKDAYS = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
    private Typeface doto;
    private LinearLayout page;
    private LocalDate selected = LocalDate.now();
    private boolean storageWritable = true, expanded = false;
    private static final Object HOLIDAY_CACHE_LOCK = new Object();
    private JSONObject holidays;
    private long holidaysCheckedAt, holidayLastAttempt;
    private boolean holidayRefreshing;
    private String holidayMessage = "Data bawaan; belum diperiksa secara online.";
    private final Handler dateHandler = new Handler(Looper.getMainLooper());
    private LocalDate displayedToday;
    private final Runnable dateTick = new Runnable() {
        @Override public void run() {
            LocalDate now = LocalDate.now();
            if (!now.equals(displayedToday)) { displayedToday = now; render(); }
            dateHandler.postDelayed(this, 60000L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open("calendar_indonesia.json"), StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) json.append(line);
            holidays = new JSONObject(json.toString());
        } catch (Exception ignored) { holidays = null; }
        loadCachedHolidays();
        if (state != null) {
            try { selected = LocalDate.parse(state.getString("selected")); } catch (Exception ignored) { selected = LocalDate.now(); }
            expanded = state.getBoolean("expanded", false);
        }
        themeChoice = getSharedPreferences("calendar_appearance", MODE_PRIVATE).getString("theme", "system");
        scroll = new ScrollView(this);
        applyPalette();
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(20), dp(16) + insets.getSystemWindowInsetTop(), dp(20), dp(24) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        LocalDate currentDay = LocalDate.now();
        if (displayedToday != null && !currentDay.equals(displayedToday)) render();
        displayedToday = currentDay; dateHandler.removeCallbacks(dateTick); dateHandler.postDelayed(dateTick, 60000L);
        refreshHolidays(true);
    }
    @Override protected void onPause() { dateHandler.removeCallbacks(dateTick); super.onPause(); }

    private void loadCachedHolidays() {
        if (!HolidayFeed.valid(holidays)) { holidays = null; return; }
        try {
            SharedPreferences prefs = getSharedPreferences("launcher_holidays", MODE_PRIVATE);
            JSONObject cached = new JSONObject(prefs.getString("feed_v1", "{}"));
            if (HolidayFeed.valid(cached) && HolidayFeed.covers(cached, holidays)
                    && cached.getLong("revision") >= holidays.getLong("revision")) {
                holidays = cached;
                holidaysCheckedAt = Math.max(0, prefs.getLong("checked_at", 0));
                holidayMessage = "Menggunakan data tersimpan di perangkat.";
            }
        } catch (Exception ignored) { }
    }
    private void refreshHolidays(boolean force) {
        if (holidays == null || holidayRefreshing) return;
        long now = System.currentTimeMillis();
        if (!force && ((holidaysCheckedAt > 0 && now >= holidaysCheckedAt && now - holidaysCheckedAt < HolidayFeed.INTERVAL_MS)
                || (holidayLastAttempt > 0 && now >= holidayLastAttempt && now - holidayLastAttempt < 300000))) return;
        holidayLastAttempt = now; holidayRefreshing = true; holidayMessage = "Memeriksa data libur online…";
        render();
        final JSONObject original = holidays;
        new Thread(() -> {
            JSONObject next = null; long checked = 0; String message;
            try {
                JSONObject downloaded = HolidayFeed.fetch();
                synchronized (HOLIDAY_CACHE_LOCK) {
                    SharedPreferences prefs = getSharedPreferences("launcher_holidays", MODE_PRIVATE);
                    JSONObject newest = original;
                    try {
                        JSONObject cached = new JSONObject(prefs.getString("feed_v1", "{}"));
                        if (HolidayFeed.valid(cached) && HolidayFeed.covers(cached, newest)
                                && cached.getLong("revision") > newest.getLong("revision")) newest = cached;
                    } catch (Exception ignored) { }
                    if (!HolidayFeed.covers(downloaded, newest) || downloaded.getLong("revision") < newest.getLong("revision")
                            || (downloaded.getLong("revision") == newest.getLong("revision") && !downloaded.toString().equals(newest.toString())))
                        throw new IllegalStateException("Older, incomplete or unversioned feed");
                    next = downloaded.getLong("revision") > newest.getLong("revision") ? downloaded : newest;
                    checked = System.currentTimeMillis();
                    boolean saved = prefs.edit().putString("feed_v1", next.toString()).putLong("checked_at", checked).commit();
                    message = saved ? "Data libur berhasil diperiksa online." : "Data online hanya sementara; penyimpanan perangkat gagal.";
                }
            } catch (Exception ignored) {
                message = "Pembaruan gagal; menggunakan data terakhir atau bawaan.";
            }
            final JSONObject result = next; final long resultTime = checked; final String resultMessage = message;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                holidayRefreshing = false; holidayMessage = resultMessage;
                if (result != null) { holidays = result; holidaysCheckedAt = resultTime; }
                render();
            });
        }, "calendar-holiday-update").start();
    }
    private String holidayStatus() {
        StringBuilder result = new StringBuilder();
        if (holidaysAvailable(selected.getYear())) result.append("Data libur tersedia: ");
        else result.append("Data libur belum tersedia untuk tahun ini; tersedia: ");
        JSONArray years = holidays == null ? null : holidays.optJSONArray("years");
        if (years == null) result.append("tidak tersedia");
        else for (int i = 0; i < years.length(); i++) { if (i > 0) result.append(", "); result.append(years.optInt(i)); }
        result.append(". ").append(holidayMessage);
        if (holidaysCheckedAt > 0) result.append(" Terakhir diperiksa: ").append(new SimpleDateFormat("dd MMM yyyy HH:mm", new Locale("id", "ID")).format(new Date(holidaysCheckedAt))).append(".");
        return result.append(" Sumber GitHub dikelola manual; jadwal bisa berubah. Peringatan bukan libur nasional, dan cuti mengikuti aturan instansi.").toString();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("selected", selected.toString());
        state.putBoolean("expanded", expanded);
        super.onSaveInstanceState(state);
    }

    private void applyPalette() {
        dark = "dark".equals(themeChoice) || (!"light".equals(themeChoice) &&
                (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        canvasColor = dark ? 0xFF101820 : 0xFFF4F7FA;
        cardColor = dark ? 0xFF19252F : Color.WHITE;
        ink = dark ? 0xFFF5F8FA : 0xFF142A3A;
        muted = dark ? 0xFFC7D3DD : 0xFF405465;
        weekend = dark ? 0xFFFF9FAA : 0xFFB3263B;
        observance = dark ? 0xFFFFC077 : 0xFF9B5700;
        controlColor = dark ? 0xFF2A3B49 : 0xFFE3EAF0;
        dayColor = dark ? 0xFF344957 : 0xFFDCE5ED;
        selectionColor = dark ? 0xFFDCE8F0 : 0xFF304355;
        selectedText = dark ? 0xFF172A38 : Color.WHITE;
        selectedWeekend = dark ? 0xFFA6152B : 0xFFFF9EA8;
        borderColor = dark ? 0xFF516776 : 0xFFC9D5DE;
        eventColor = dark ? 0xFF263743 : 0xFFE5ECF2;
        todayColor = dark ? cardColor : Color.WHITE;
        getWindow().setStatusBarColor(canvasColor); getWindow().setNavigationBarColor(canvasColor);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (scroll != null) scroll.setBackgroundColor(canvasColor);
    }
    private AlertDialog.Builder dialog() {
        return new AlertDialog.Builder(this, dark ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert);
    }
    private void styleDialog(AlertDialog popup) {
        TextView message = popup.findViewById(android.R.id.message);
        if (message != null) message.setTypeface(doto);
        int titleId = getResources().getIdentifier("alertTitle", "id", "android");
        View title = titleId == 0 ? null : popup.findViewById(titleId);
        if (title instanceof TextView) ((TextView) title).setTypeface(doto);
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
            TextView button = popup.getButton(which); if (button != null) button.setTypeface(doto);
        }
        android.widget.ListView list = popup.getListView();
        if (list != null) list.post(() -> {
            for (int i = 0; i < list.getChildCount(); i++) {
                View child = list.getChildAt(i); if (child instanceof TextView) ((TextView) child).setTypeface(doto);
            }
        });
    }
    private void showDialog(AlertDialog popup) { popup.setOnShowListener(d -> styleDialog(popup)); popup.show(); }
    private String themeLabel() { return "dark".equals(themeChoice) ? "Gelap" : "light".equals(themeChoice) ? "Terang" : "Sistem"; }
    private int themeIcon(String value) {
        return "dark".equals(value) ? R.drawable.ic_calendar_theme_dark : "light".equals(value) ? R.drawable.ic_calendar_theme_light : R.drawable.ic_calendar_theme_system;
    }
    private ImageButton themeButton(String value, boolean selected) {
        ImageButton button = new ImageButton(this); button.setImageResource(themeIcon(value));
        button.setColorFilter(selected ? selectedText : ink); button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setPadding(dp(12),dp(12),dp(12),dp(12));
        button.setBackground(selected ? bg(selectionColor) : bg(controlColor));
        String label = "dark".equals(value) ? "Gelap" : "light".equals(value) ? "Terang" : "Ikuti perangkat";
        button.setContentDescription("Tema " + label + (selected ? ", dipilih" : ""));
        button.setTooltipText("Tema " + label);
        button.setSelected(selected); return button;
    }
    private void chooseTheme(View anchor) {
        final String[] values = {"system", "light", "dark"};
        LinearLayout choices = new LinearLayout(this); choices.setGravity(Gravity.CENTER);
        choices.setPadding(dp(4),dp(4),dp(4),dp(4));
        PopupWindow popup = new PopupWindow(choices, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(bg(cardColor)); popup.setOutsideTouchable(true); popup.setElevation(dp(6));
        for (String value : values) {
            ImageButton option = themeButton(value, value.equals(themeChoice));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(48), dp(48)); size.setMargins(dp(2),0,dp(2),0);
            choices.addView(option, size);
            option.setOnClickListener(v -> {
                themeChoice = value; getSharedPreferences("calendar_appearance", MODE_PRIVATE).edit().putString("theme", themeChoice).apply();
                popup.dismiss(); applyPalette(); render();
            });
        }
        popup.showAsDropDown(anchor, 0, 0, Gravity.END);
    }
    private void changePeriod(int direction) { selected = expanded ? CalendarDates.shiftMonth(selected, direction) : CalendarDates.shiftWeek(selected, direction); render(); }
    private void chooseMonth() {
        showDialog(dialog().setTitle(MONTHS[selected.getMonthValue()-1] + " " + selected.getYear())
                .setItems(new String[]{expanded ? "Bulan sebelumnya" : "Minggu sebelumnya", expanded ? "Bulan berikutnya" : "Minggu berikutnya", "Hari ini"}, (d, choice) -> {
                    if (choice == 2) { selected = LocalDate.now(); render(); }
                    else changePeriod(choice == 0 ? -1 : 1);
                }).setNegativeButton("Batal", null).create());
    }
    private final class MonthGrid extends LinearLayout {
        private float downX, downY; private boolean horizontal, cancelled;
        MonthGrid() { super(CalendarActivity.this); setOrientation(VERTICAL); setClickable(true); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override public boolean onInterceptTouchEvent(MotionEvent e) {
            int action = e.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) { downX=e.getX(); downY=e.getY(); horizontal=false; cancelled=false; }
            if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_CANCEL) cancelled=true;
            if (action == MotionEvent.ACTION_MOVE && !cancelled && CalendarSwipe.direction(e.getX()-downX,e.getY()-downY,dp(36)) != 0) {
                horizontal=true; getParent().requestDisallowInterceptTouchEvent(true); return true;
            }
            return super.onInterceptTouchEvent(e);
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (!horizontal) return super.onTouchEvent(e);
            if (e.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN || e.getActionMasked() == MotionEvent.ACTION_CANCEL) { cancelled=true; return true; }
            if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                int direction = cancelled ? 0 : CalendarSwipe.direction(e.getX()-downX,e.getY()-downY,dp(48));
                horizontal=false; if (direction != 0) changePeriod(direction); return true;
            }
            return true;
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(doto); view.setFontVariationSettings("'wght' 800");
        return view;
    }
    private GradientDrawable bg(int color) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12)); d.setStroke(dp(1), borderColor); return d;
    }
    private TextView button(String label, Runnable click) {
        TextView v = text(label, 13, ink); v.setGravity(Gravity.CENTER); v.setMinHeight(dp(48));
        v.setPadding(dp(8), dp(8), dp(8), dp(8)); v.setBackground(bg(controlColor));
        v.setOnClickListener(w -> click.run()); return v;
    }
    private void space(int height) { page.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private String dateLabel(LocalDate date) { return date.getDayOfMonth() + " " + MONTHS[date.getMonthValue()-1] + " " + date.getYear(); }
    private JSONArray readEvents() {
        try {
            String json = getSharedPreferences("launcher_calendar", MODE_PRIVATE).getString("events_v1", "[]");
            JSONArray events = new JSONArray(json);
            for (int i = 0; i < events.length(); i++) {
                JSONObject e = events.getJSONObject(i);
                String id = e.getString("id"), date = e.getString("date"), title = e.getString("title"), time = e.getString("time");
                LocalDate parsed = LocalDate.parse(date);
                if (id.isEmpty() || parsed.getYear() < 1900 || parsed.getYear() > 2100 ||
                        !CalendarDates.validTitle(title) || !CalendarDates.validTime(time)) throw new IllegalArgumentException();
            }
            return events;
        } catch (Exception e) {
            storageWritable = false;
            return new JSONArray();
        }
    }
    private boolean saveEvents(JSONArray events) {
        if (!storageWritable) return false;
        try {
            if (getSharedPreferences("launcher_calendar", MODE_PRIVATE).edit().putString("events_v1", events.toString()).commit()) return true;
        } catch (Exception ignored) { }
        storageWritable = false;
        return false;
    }
    private void storageError() {
        Toast.makeText(this, "Penyimpanan kalender bermasalah. Acara tidak berhasil disimpan.", Toast.LENGTH_LONG).show();
        render();
    }
    private boolean holidaysAvailable(int year) {
        if (holidays == null) return false;
        JSONArray years = holidays.optJSONArray("years");
        if (years != null) for (int i = 0; i < years.length(); i++) if (years.optInt(i) == year) return true;
        return false;
    }
    private JSONArray holidaysFor(LocalDate date) {
        JSONObject entries = holidays == null ? null : holidays.optJSONObject("entries");
        JSONArray found = entries == null ? null : entries.optJSONArray(date.toString());
        return found == null ? new JSONArray() : found;
    }
    private String holidayText(JSONObject event) {
        String type = event.optString("type");
        String label = "libur".equals(type) ? "Libur nasional" : "cuti".equals(type) ? "Cuti bersama" : "Peringatan (bukan libur nasional)";
        return label + " · " + event.optString("name");
    }
    private boolean hasHolidayType(JSONArray events, String type) {
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event != null && type.equals(event.optString("type"))) return true;
        }
        return false;
    }
    private Drawable dayBackground(int fill, boolean isToday, boolean chosen, boolean holiday, boolean observanceDay, boolean event) {
        int[] marks = new int[3]; int count = 0;
        if (holiday) marks[count++] = chosen ? selectedWeekend : weekend;
        if (observanceDay) marks[count++] = chosen ? (dark ? 0xFF9B5700 : 0xFFFFC077) : observance;
        if (event) marks[count++] = chosen ? (dark ? 0xFF0064A5 : 0xFF77C8FF) : (dark ? 0xFF77C8FF : 0xFF0064A5);
        if (count == 0 && isToday) marks[count++] = borderColor;
        Drawable[] parts = new Drawable[count + 1];
        GradientDrawable base = new GradientDrawable(); base.setColor(fill); base.setCornerRadius(dp(11)); parts[0] = base;
        for (int i=0; i<count; i++) {
            GradientDrawable ring = new GradientDrawable(); ring.setColor(Color.TRANSPARENT);
            ring.setCornerRadius(dp(Math.max(4,11-i*2))); ring.setStroke(dp(2), marks[i]); parts[i+1] = ring;
        }
        LayerDrawable background = new LayerDrawable(parts);
        for (int i=0; i<count; i++) { int inset=dp(i*2); background.setLayerInset(i+1,inset,inset,inset,inset); }
        return background;
    }
    private ImageButton actionIcon(int resource, String description, Runnable click) {
        ImageButton action = new ImageButton(this); action.setImageResource(resource); action.setColorFilter(ink);
        action.setScaleType(ImageView.ScaleType.FIT_CENTER); action.setPadding(dp(12),dp(12),dp(12),dp(12));
        action.setBackgroundColor(Color.TRANSPARENT); action.setContentDescription(description); action.setTooltipText(description);
        action.setOnClickListener(v -> click.run()); return action;
    }
    private void addHolidayEntries(LocalDate date, boolean showDate) {
        JSONArray events = holidaysFor(date);
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            String prefix = showDate ? date.getDayOfMonth() + " · " : "";
            page.addView(text(prefix + holidayText(event), 13, "libur".equals(event.optString("type")) ? weekend : observance));
            space(5);
        }
    }
    private boolean hasEvents(JSONArray events, LocalDate date) {
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            if (e != null && date.toString().equals(e.optString("date"))) return true;
        }
        return false;
    }

    private void render() {
        page.removeAllViews();
        LocalDate actualToday = LocalDate.now();
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back = actionIcon(R.drawable.ic_calendar_back, "Kembali", this::finish);
        top.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        TextView year = text(String.valueOf(selected.getYear()), 17, ink);
        year.setContentDescription("Tahun " + selected.getYear()); top.addView(year);
        top.addView(new View(this),new LinearLayout.LayoutParams(0,dp(1),1));
        ImageButton theme = themeButton(themeChoice, false);
        theme.setBackgroundColor(Color.TRANSPARENT);
        theme.setContentDescription("Tema: " + themeLabel() + ". Pilih tema kalender");
        theme.setTooltipText("Pilih tema kalender");
        theme.setOnClickListener(this::chooseTheme);
        top.addView(theme,new LinearLayout.LayoutParams(dp(48),dp(48))); page.addView(top); space(8);
        LinearLayout root = page;
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8),dp(8),dp(8),dp(8)); card.setBackground(bg(cardColor));
        root.addView(card); page = card;
        TextView month = button(MONTHS[selected.getMonthValue()-1], this::chooseMonth);
        month.setContentDescription("Pilih bulan. " + MONTHS[selected.getMonthValue()-1] + " " + selected.getYear());
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.addView(month,new LinearLayout.LayoutParams(-2,dp(48)));
        nav.addView(new View(this),new LinearLayout.LayoutParams(0,dp(1),1));
        nav.addView(actionIcon(R.drawable.ic_calendar_add,"Tambah acara",this::addEvent),new LinearLayout.LayoutParams(dp(48),dp(48)));
        page.addView(nav); space(8);
        MonthGrid grid = new MonthGrid(); page.addView(grid);
        LinearLayout weekdays = new LinearLayout(this);
        for (int col = 0; col < WEEKDAYS.length; col++) {
            TextView title = text(WEEKDAYS[col], 12, col >= 5 ? weekend : ink); title.setFontVariationSettings("'wght' 900"); title.setGravity(Gravity.CENTER);
            weekdays.addView(title, new LinearLayout.LayoutParams(0, dp(30), 1));
        }
        grid.addView(weekdays);
        JSONArray events = readEvents();
        YearMonth monthValue = YearMonth.from(selected);
        int leading = CalendarDates.leadingDays(monthValue), length = monthValue.lengthOfMonth();
        int rowCount = expanded ? (leading + length + 6) / 7 : 1;
        LocalDate weekStart = CalendarDates.weekStart(selected);
        for (int row = 0; row < rowCount; row++) {
            LinearLayout week = new LinearLayout(this);
            for (int col = 0; col < 7; col++) {
                int dayNumber = row * 7 + col - leading + 1;
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, dp(44), 1);
                LocalDate day = expanded ? (dayNumber < 1 || dayNumber > length ? null : monthValue.atDay(dayNumber)) : weekStart.plusDays(col);
                if (day == null || !CalendarDates.inRange(day)) { week.addView(new View(this), cell); continue; }
                dayNumber = day.getDayOfMonth();
                boolean has = hasEvents(events, day);
                JSONArray dayHolidays = holidaysFor(day);
                boolean isHoliday = hasHolidayType(dayHolidays, "libur");
                boolean isObservance = hasHolidayType(dayHolidays, "peringatan");
                FrameLayout item = new FrameLayout(this);
                boolean chosen = day.equals(selected);
                TextView number = text(String.valueOf(dayNumber), 14,
                        col >= 5 || isHoliday ? (chosen ? selectedWeekend : weekend) : (chosen ? selectedText : ink));
                number.setGravity(Gravity.CENTER);
                number.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                number.setBackground(dayBackground(chosen ? selectionColor : day.equals(actualToday) ? todayColor : dayColor, day.equals(actualToday), chosen, isHoliday, isObservance, has));
                item.addView(number, new FrameLayout.LayoutParams(dp(38), dp(38), Gravity.CENTER));
                StringBuilder description = new StringBuilder(dateLabel(day));
                for (int i = 0; i < dayHolidays.length(); i++) {
                    JSONObject event = dayHolidays.optJSONObject(i);
                    if (event != null) description.append(", ").append(holidayText(event));
                }
                if (has) description.append(", ada acara");
                item.setContentDescription(description.toString());
                item.setFocusable(true);
                item.setSelected(day.equals(selected));
                item.setOnClickListener(v -> { selected = day; render(); }); week.addView(item, cell);
            }
            grid.addView(week);
        }
        ImageButton expand = actionIcon(R.drawable.ic_calendar_expand, expanded ? "Tampilkan satu minggu" : "Tampilkan satu bulan", () -> { expanded = !expanded; render(); });
        expand.setRotation(expanded ? 180f : 0f);
        LinearLayout toggleRow = new LinearLayout(this); toggleRow.setGravity(Gravity.CENTER);
        toggleRow.addView(expand,new LinearLayout.LayoutParams(dp(48),dp(48))); page.addView(toggleRow);
        page = root; space(16);
        LinearLayout agenda = new LinearLayout(this); agenda.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout agendaTitle = new LinearLayout(this); agendaTitle.setOrientation(LinearLayout.VERTICAL);
        agendaTitle.addView(text(selected.equals(actualToday) ? "HARI INI" : "TANGGAL DIPILIH", 11, muted));
        agendaTitle.addView(text(dateLabel(selected), 19, ink));
        agenda.addView(agendaTitle, new LinearLayout.LayoutParams(0,-2,1));
        agenda.addView(button("Hari ini", () -> { selected = LocalDate.now(); render(); }));
        page.addView(agenda); space(10);
        addHolidayEntries(selected, false);
        space(4);
        TextView agendaHeading = text(expanded ? "ACARA HARI INI" : "ACARA MINGGU TERPILIH", 12, muted);
        page.addView(agendaHeading); space(8);
        ArrayList<JSONObject> visibleEvents = new ArrayList<>();
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i); if (event == null) continue;
            try {
                if (CalendarDates.inAgenda(LocalDate.parse(event.optString("date")), selected, actualToday, expanded)) visibleEvents.add(event);
            } catch (Exception ignored) { }
        }
        visibleEvents.sort(Comparator.comparing((JSONObject e) -> e.optString("date"))
                .thenComparing(e -> e.optString("time").isEmpty() ? "99:99" : e.optString("time")));
        for (JSONObject e : visibleEvents) {
            String id = e.optString("id"), title = e.optString("title"), time = e.optString("time");
            LocalDate eventDay = LocalDate.parse(e.optString("date"));
            String when = eventDay.getDayOfMonth() + " " + MONTHS[eventDay.getMonthValue()-1].substring(0,3) + " " + eventDay.getYear();
            if (!time.isEmpty()) when += " · " + time;
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(12), dp(8)); row.setBackground(bg(eventColor));
            TextView label = text(when + "\n" + title, 14, ink);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            TextView remove = button("Hapus", () -> confirmDelete(id, title));
            row.addView(remove, new LinearLayout.LayoutParams(-2, -2)); page.addView(row); space(8);
        }
        if (visibleEvents.isEmpty()) page.addView(text(expanded ? "Belum ada acara hari ini." : "Belum ada acara minggu ini.", 13, muted));
        if (!holidaysAvailable(selected.getYear())) { space(8); page.addView(text("Data libur belum tersedia untuk tahun ini.", 12, muted)); }
        if (holidayMessage.startsWith("Pembaruan gagal")) { space(8); page.addView(text("Data libur offline; memakai data tersimpan atau bawaan.", 12, muted)); }
        if (!storageWritable) { space(8); page.addView(text("Penyimpanan kalender bermasalah; perubahan tidak dapat disimpan.", 12, weekend)); }
        space(16); page.addView(actionIcon(R.drawable.ic_calendar_info, "Info kalender", () -> showInfo(monthValue)), new LinearLayout.LayoutParams(dp(48),dp(48)));

    }

    private void showInfo(YearMonth month) {
        StringBuilder info = new StringBuilder("Garis merah: libur nasional. Garis amber: peringatan. Garis biru: acara. Beberapa garis berarti beberapa penanda.\n\nLibur dan peringatan bulan ini:\n");
        boolean found = false;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            JSONArray entries = holidaysFor(month.atDay(day));
            for (int i=0; i<entries.length(); i++) {
                JSONObject e = entries.optJSONObject(i); if (e == null) continue;
                info.append(day).append(" · ").append(holidayText(e)).append("\n"); found = true;
            }
        }
        if (!found) info.append("Tidak ada data untuk bulan ini.\n");
        info.append("\n").append(holidayStatus());
        info.append("\n\nAcara hanya disimpan di perangkat; tanpa sinkronisasi, cadangan atau pengingat. Menghapus data aplikasi akan menghapus acara.");
        showDialog(dialog().setTitle("Info kalender").setMessage(info.toString()).setNegativeButton("Tutup",null)
                .setPositiveButton("Perbarui libur", (d,w) -> refreshHolidays(true)).create());
    }

    private void addEvent() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText title = new EditText(this); title.setTypeface(doto); title.setSingleLine(true); title.setHint("Nama acara");
        title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)}); form.addView(title);
        EditText time = new EditText(this); time.setTypeface(doto); time.setSingleLine(true);
        time.setHint("Waktu HH:mm (opsional)"); time.setInputType(InputType.TYPE_CLASS_TEXT); time.setTypeface(doto); form.addView(time);
        AlertDialog dialog = dialog().setTitle("Acara · " + dateLabel(selected)).setView(form)
                .setNegativeButton("Batal", null).setPositiveButton("Simpan", null).create();
        dialog.setOnShowListener(ignored -> {
            styleDialog(dialog);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTypeface(doto);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTypeface(doto);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = title.getText().toString().trim(), clock = time.getText().toString().trim();
                if (!CalendarDates.validTitle(value)) { title.setError("Isi nama acara (maks. 80 karakter)"); return; }
                if (!CalendarDates.validTime(clock)) { time.setError("Gunakan HH:mm, misalnya 09:30"); return; }
                JSONArray events = readEvents();
                try {
                    JSONObject event = new JSONObject(); event.put("id", UUID.randomUUID().toString());
                    event.put("date", selected.toString()); event.put("title", value); event.put("time", clock);
                    events.put(event);
                    if (!saveEvents(events)) { storageError(); return; }
                    dialog.dismiss(); render();
                } catch (JSONException ignoredError) { title.setError("Acara tidak dapat disimpan"); }
            });
        });
        dialog.show();
    }

    private void confirmDelete(String id, String title) {
        AlertDialog dialog = dialog().setMessage("Hapus acara \"" + title + "\"?")
                .setNegativeButton("Batal", null).setPositiveButton("Hapus", (d, w) -> {
                    JSONArray old = readEvents(), kept = new JSONArray();
                    for (int i = 0; i < old.length(); i++) {
                        JSONObject event = old.optJSONObject(i);
                        if (event != null && !id.equals(event.optString("id"))) kept.put(event);
                    }
                    if (!saveEvents(kept)) { storageError(); return; }
                    render();
                }).create();
        dialog.setOnShowListener(ignored -> {
            styleDialog(dialog);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTypeface(doto);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTypeface(doto);
        });
        dialog.show();
    }
}
