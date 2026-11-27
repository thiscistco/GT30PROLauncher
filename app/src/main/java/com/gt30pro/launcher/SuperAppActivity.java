package com.gt30pro.launcher;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/** Pusat aplikasi bawaan: ikon putih dalam grid, tanpa label visual. */
public class SuperAppActivity extends SwipeBackActivity {
    private WeatherController weather;
    private AppUi ui;
    private WeatherView weatherView;
    @Override protected boolean supportsBottomSwipe() { return true; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_app);
        ui = new AppUi(this); ui.window(this);

        View root = findViewById(R.id.root);
        root.setBackgroundColor(ui.canvas);
        android.widget.LinearLayout content = findViewById(R.id.content);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            content.setPadding(dp(20), dp(16) + insets.getSystemWindowInsetTop(),
                    dp(20), dp(24) + insets.getSystemWindowInsetBottom());
            return insets;
        });

        Typeface doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        content.removeView(findViewById(R.id.back)); content.removeView(findViewById(R.id.super_title));
        content.addView(ui.header(this, doto, "Super App"),0);

        GridLayout container = findViewById(R.id.modules_container);
        addModule(container, R.drawable.ic_module_clock, "Jam", ClockActivity.class);
        addModule(container, R.drawable.ic_module_calendar, "Kalender", CalendarActivity.class);
        weatherView = new WeatherView(this, false);
        weatherView.setIconColor(ui.ink);
        GridLayout.LayoutParams weatherParams = new GridLayout.LayoutParams();
        weatherParams.width = 1; weatherParams.height = dp(76);
        container.addView(weatherView, weatherParams);
        weather = new WeatherController(this, weatherView);
        weatherView.setOpensDetails(true);
        weatherView.setContentDescription("Buka Cuaca");
        weatherView.setOnClickListener(v -> startActivity(new Intent(this, WeatherActivity.class)));
        container.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL) sizeGridCells(container);
        });
        container.post(() -> sizeGridCells(container));
    }

    @Override protected void onResume() { super.onResume(); AppUi current = new AppUi(this); if (ui != null && (ui.dark != current.dark || !ui.choice.equals(current.choice))) { recreate(); return; } if (weather != null) weather.resume(); }
    @Override protected void onPause() { if (weather != null) weather.pause(); super.onPause(); }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (weather != null) weather.permissionResult(requestCode);
    }

    private void addModule(GridLayout grid, int icon, String name, Class<?> activity) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(icon);
        button.setColorFilter(ui.ink);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setPadding(dp(13), dp(13), dp(13), dp(13));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setContentDescription(name);
        button.setOnClickListener(v -> startActivity(new Intent(this, activity)));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 1;
        params.height = dp(76);
        grid.addView(button, params);
    }

    private void sizeGridCells(GridLayout grid) {
        int width = grid.getWidth();
        if (width <= 0) return;
        int cellWidth = Math.max(1, width / 4);
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) child.getLayoutParams();
            if (params.width != cellWidth) {
                params.width = cellWidth;
                child.setLayoutParams(params);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
