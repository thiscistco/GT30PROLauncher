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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** Kalender lokal; data tidak disinkronkan dan tidak menjadwalkan pengingat. */
public class CalendarActivity extends SwipeBackActivity {
    private static final int AMBER = 0xFFFFD49D, MUTED = 0xFFE7A579;
    private static final String[] MONTHS = {"Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
    private static final String[] WEEKDAYS = {"Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"};
    private Typeface doto;
    private LinearLayout page;
    private LocalDate selected = LocalDate.now();
    private boolean storageWritable = true;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
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
        for (String day : WEEKDAYS) {
            TextView title = text(day, 11, MUTED); title.setGravity(Gravity.CENTER);
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
                TextView item = text(String.valueOf(dayNumber) + (has ? "·" : ""), 14, day.equals(LocalDate.now()) ? 0xFFFFE5B1 : AMBER);
                item.setGravity(Gravity.CENTER); item.setContentDescription(dateLabel(day) + (has ? ", ada acara" : ""));
                if (day.equals(selected)) item.setBackground(bg(0xFF61402C));
                else if (day.equals(LocalDate.now())) item.setBackground(bg(0xFF211A17));
                item.setSelected(day.equals(selected));
                item.setOnClickListener(v -> { selected = day; render(); }); week.addView(item, cell);
            }
            page.addView(week);
        }
        space(12); page.addView(button("Hari ini", () -> { selected = LocalDate.now(); render(); })); space(16);
        page.addView(text(dateLabel(selected), 19, AMBER)); space(10);
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
