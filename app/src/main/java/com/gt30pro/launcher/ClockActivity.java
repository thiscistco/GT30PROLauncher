package com.gt30pro.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Modul jam lokal Super App. Waktu mengikuti zona waktu perangkat. */
public class ClockActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timeView;
    private TextView dateView;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            Date now = new Date();
            timeView.setText(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now));
            dateView.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID")).format(now));
            handler.postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Typeface doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(10, 12, 15));
        ImageView wallpaper = new ImageView(this);
        wallpaper.setImageBitmap(Wallpapers.load(this));
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallpaper.setAlpha(0.18f);
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(24), dp(24) + insets.getSystemWindowInsetTop(),
                    dp(24), dp(24) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        TextView back = text("←  Kembali", 14, 0xFFFFC28B, doto);
        back.setMinHeight(dp(48));
        back.setGravity(Gravity.CENTER_VERTICAL);
        back.setOnClickListener(v -> finish());
        page.addView(back, new LinearLayout.LayoutParams(-2, -2));
        TextView eyebrow = text("SUPER APP", 12, 0xFFE7A579, doto);
        LinearLayout.LayoutParams eyebrowParams = new LinearLayout.LayoutParams(-2, -2);
        eyebrowParams.topMargin = dp(30);
        page.addView(eyebrow, eyebrowParams);
        TextView heading = text("JAM", 30, 0xFFFFE5B1, doto);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-2, -2);
        headingParams.topMargin = dp(8);
        page.addView(heading, headingParams);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(16), dp(32), dp(16), dp(32));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE61B1819);
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), 0x77E7A579);
        card.setBackground(bg);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(28);
        page.addView(card, cardParams);
        TextView label = text("WAKTU SAAT INI", 12, 0xFFE7A579, doto);
        label.setGravity(Gravity.CENTER);
        card.addView(label);
        timeView = text("--:--:--", 34, 0xFFFFD49D, doto);
        timeView.setGravity(Gravity.CENTER);
        timeView.setShadowLayer(dp(8), 0, 0, 0xA6DD6234);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(-1, -2);
        timeParams.topMargin = dp(18);
        card.addView(timeView, timeParams);
        dateView = text("", 13, 0xFFEED3BD, doto);
        dateView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2);
        dateParams.topMargin = dp(12);
        card.addView(dateView, dateParams);
        setContentView(root);
    }

    private TextView text(String value, int size, int color, Typeface font) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(font);
        view.setFontVariationSettings("'wght' 700");
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onResume() { super.onResume(); handler.removeCallbacks(tick); tick.run(); }
    @Override protected void onPause() { handler.removeCallbacks(tick); super.onPause(); }
}
