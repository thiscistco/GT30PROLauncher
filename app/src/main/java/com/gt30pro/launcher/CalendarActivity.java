package com.gt30pro.launcher;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** Kalender lokal; data tidak disinkronkan dan tidak menjadwalkan pengingat. */
public class CalendarActivity extends SwipeBackActivity {
    @Override protected boolean supportsBottomSwipe() { return true; }
    private static final int AMBER = 0xFFFFD49D, MUTED = 0xFFE7A579, WEEKEND = 0xFFFF6B6B;
    private static final String[] MONTHS = {"Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
    private static final String[] WEEKDAYS = {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"};
    private Typeface doto;
    private LinearLayout page;
    private LocalDate selected = LocalDate.now();
    private boolean storageWritable = true;
    private JSONObject holidays;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open("calendar_indonesia.json"), StandardCharsets.UTF_8))) {
            StringBuilder json = new StringBuilder(); String line;
            while ((line = reader.readLine()) != null) json.append(line);
            holidays = new JSONObject(json.toString());
        } catch (Exception ignored) { holidays = null; }
        if (state != null) {
            try { selected = LocalDate.parse(state.getString("selected")); } catch (Exception ignored) { selected = LocalDate.now(); }
        }
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
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

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("selected", selected.toString());
        super.onSaveInstanceState(state);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setTypeface(doto); view.setFontVariationSettings("'wght' 700");
        return view;
    }
    private GradientDrawable bg(int color) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12)); d.setStroke(dp(1), 0x77E7A579); return d;
    }
    private TextView button(String label, Runnable click) {
        TextView v = text(label, 13, AMBER); v.setGravity(Gravity.CENTER); v.setMinHeight(dp(48));
        v.setPadding(dp(8), dp(8), dp(8), dp(8)); v.setBackground(bg(0xFF211A17));
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
    private View markerDot(int color) {
        View dot = new View(this);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL); circle.setColor(color);
        dot.setBackground(circle);
        dot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return dot;
    }
    private void addHolidayEntries(LocalDate date, boolean showDate) {
        JSONArray events = holidaysFor(date);
        for (int i = 0; i < events.length(); i++) {
            JSONObject event = events.optJSONObject(i);
            if (event == null) continue;
            String prefix = showDate ? date.getDayOfMonth() + " · " : "";
            page.addView(text(prefix + holidayText(event), 13, "libur".equals(event.optString("type")) ? WEEKEND : AMBER));
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
        TextView back = text("←  Kembali", 14, AMBER); back.setMinHeight(dp(48)); back.setGravity(Gravity.CENTER_VERTICAL);
        back.setOnClickListener(v -> finish()); page.addView(back);
        page.addView(text("SUPER APP", 12, MUTED));
        page.addView(text("KALENDER", 30, 0xFFFFE5B1)); space(16);
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView previous = button("<", () -> { selected = CalendarDates.shiftMonth(selected, -1); render(); });
        previous.setContentDescription("Bulan sebelumnya");
        nav.addView(previous, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView month = text(MONTHS[selected.getMonthValue()-1] + " " + selected.getYear(), 17, AMBER);
        month.setGravity(Gravity.CENTER); nav.addView(month, new LinearLayout.LayoutParams(0, -2, 1));
        TextView next = button(">", () -> { selected = CalendarDates.shiftMonth(selected, 1); render(); });
        next.setContentDescription("Bulan berikutnya");
        previous.setEnabled(!(selected.getYear() == 1900 && selected.getMonthValue() == 1));
        next.setEnabled(!(selected.getYear() == 2100 && selected.getMonthValue() == 12));
        nav.addView(next, new LinearLayout.LayoutParams(dp(48), dp(48))); page.addView(nav); space(12);
        LinearLayout weekdays = new LinearLayout(this);
        for (int col = 0; col < WEEKDAYS.length; col++) {
            TextView title = text(WEEKDAYS[col], 11, col >= 5 ? WEEKEND : MUTED); title.setGravity(Gravity.CENTER);
            weekdays.addView(title, new LinearLayout.LayoutParams(0, dp(30), 1));
        }
        page.addView(weekdays);
        JSONArray events = readEvents();
        YearMonth monthValue = YearMonth.from(selected);
        int leading = CalendarDates.leadingDays(monthValue), length = monthValue.lengthOfMonth();
        int rowCount = (leading + length + 6) / 7;
        for (int row = 0; row < rowCount; row++) {
            LinearLayout week = new LinearLayout(this);
            for (int col = 0; col < 7; col++) {
                int dayNumber = row * 7 + col - leading + 1;
                LinearLayout.LayoutParams cell = new LinearLayout.LayoutParams(0, dp(44), 1);
                if (dayNumber < 1 || dayNumber > length) { week.addView(new View(this), cell); continue; }
                LocalDate day = monthValue.atDay(dayNumber);
                boolean has = hasEvents(events, day);
                JSONArray dayHolidays = holidaysFor(day);
                boolean isHoliday = hasHolidayType(dayHolidays, "libur");
                boolean isObservance = hasHolidayType(dayHolidays, "peringatan");
                FrameLayout item = new FrameLayout(this);
                TextView number = text(String.valueOf(dayNumber) + (has ? "·" : ""), 14,
                        col >= 5 || isHoliday ? WEEKEND : (day.equals(LocalDate.now()) ? 0xFFFFE5B1 : AMBER));
                number.setGravity(Gravity.CENTER);
                number.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                item.addView(number, new FrameLayout.LayoutParams(-1, -1));
                if (isHoliday || isObservance) {
                    LinearLayout dots = new LinearLayout(this);
                    dots.setGravity(Gravity.CENTER);
                    dots.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    if (isHoliday) dots.addView(markerDot(WEEKEND), new LinearLayout.LayoutParams(dp(5), dp(5)));
                    if (isObservance) {
                        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(5), dp(5));
                        if (isHoliday) dotParams.leftMargin = dp(3);
                        dots.addView(markerDot(AMBER), dotParams);
                    }
                    FrameLayout.LayoutParams dotPosition = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                    dotPosition.bottomMargin = dp(4);
                    item.addView(dots, dotPosition);
                }
                StringBuilder description = new StringBuilder(dateLabel(day));
                for (int i = 0; i < dayHolidays.length(); i++) {
                    JSONObject event = dayHolidays.optJSONObject(i);
                    if (event != null) description.append(", ").append(holidayText(event));
                }
                if (has) description.append(", ada acara");
                item.setContentDescription(description.toString());
                item.setFocusable(true);
                if (day.equals(selected)) item.setBackground(bg(0xFF61402C));
                else if (day.equals(LocalDate.now())) item.setBackground(bg(0xFF211A17));
                item.setSelected(day.equals(selected));
                item.setOnClickListener(v -> { selected = day; render(); }); week.addView(item, cell);
            }
            page.addView(week);
        }
        space(6); page.addView(text("Titik merah: libur nasional · titik amber: peringatan", 11, MUTED));
        space(6); page.addView(button("Hari ini", () -> { selected = LocalDate.now(); render(); })); space(16);
        page.addView(text(dateLabel(selected), 19, AMBER)); space(10);
        addHolidayEntries(selected, false);
        page.addView(button("+ Tambah acara", this::addEvent)); space(12);
        boolean found = false;
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.optJSONObject(i);
            if (e == null || !selected.toString().equals(e.optString("date"))) continue;
            found = true;
            String id = e.optString("id"), title = e.optString("title"), time = e.optString("time");
            LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(8), dp(12), dp(8)); row.setBackground(bg(0xFF191717));
            TextView label = text((time.isEmpty() ? "" : time + " · ") + title, 14, AMBER);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            TextView remove = button("Hapus", () -> confirmDelete(id, title));
            row.addView(remove, new LinearLayout.LayoutParams(-2, -2)); page.addView(row); space(8);
        }
        if (!found) page.addView(text("Belum ada acara pada tanggal ini.", 13, MUTED));
        space(16); page.addView(text("LIBUR & PERINGATAN", 19, AMBER)); space(10);
        if (holidaysAvailable(selected.getYear())) {
            boolean foundHoliday = false;
            for (int day = 1; day <= monthValue.lengthOfMonth(); day++) {
                LocalDate date = monthValue.atDay(day);
                if (holidaysFor(date).length() > 0) { addHolidayEntries(date, true); foundHoliday = true; }
            }
            if (!foundHoliday) page.addView(text("Tidak ada data libur atau peringatan bulan ini.", 13, MUTED));
            space(8); page.addView(text("Data 2026–2027 berdasarkan SKB, tidak diperbarui otomatis. Idulfitri dan Iduladha 2027 menunggu penetapan Kemenag. Peringatan bukan libur nasional; cuti mengikuti aturan instansi.", 12, MUTED));
        } else {
            page.addView(text(holidays == null ? "Data libur tidak dapat dibaca." : "Data libur dan peringatan belum tersedia untuk tahun ini (tersedia 2026–2027).", 12, MUTED));
        }
        space(12); page.addView(text(storageWritable
                ? "Acara disimpan di perangkat ini; tidak ada sinkronisasi atau pengingat."
                : "Penyimpanan kalender bermasalah; data lama tidak ditimpa dan perubahan tidak dapat disimpan.", 12, MUTED));
    }

    private void addEvent() {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText title = new EditText(this); title.setTypeface(doto); title.setSingleLine(true); title.setHint("Nama acara");
        title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)}); form.addView(title);
        EditText time = new EditText(this); time.setTypeface(doto); time.setSingleLine(true);
        time.setHint("Waktu HH:mm (opsional)"); time.setInputType(InputType.TYPE_CLASS_TEXT); time.setTypeface(doto); form.addView(time);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Acara · " + dateLabel(selected)).setView(form)
                .setNegativeButton("Batal", null).setPositiveButton("Simpan", null).create();
        dialog.setOnShowListener(ignored -> {
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
        AlertDialog dialog = new AlertDialog.Builder(this).setMessage("Hapus acara \"" + title + "\"?")
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
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTypeface(doto);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTypeface(doto);
        });
        dialog.show();
    }
}
