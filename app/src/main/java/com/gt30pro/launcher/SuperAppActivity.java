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
    @Override protected boolean supportsBottomSwipe() { return true; }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_app);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        View root = findViewById(R.id.root);
        View content = findViewById(R.id.content);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            content.setPadding(content.getPaddingLeft(), insets.getSystemWindowInsetTop(),
                    content.getPaddingRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });

        Typeface doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        ((TextView) findViewById(R.id.back)).setTypeface(doto);
        ((TextView) findViewById(R.id.super_title)).setTypeface(doto);
        findViewById(R.id.back).setOnClickListener(v -> finish());

        GridLayout container = findViewById(R.id.modules_container);
        addModule(container, R.drawable.ic_module_clock, "Jam", ClockActivity.class);
        addModule(container, R.drawable.ic_module_calendar, "Kalender", CalendarActivity.class);
        container.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL) sizeGridCells(container);
        });
        container.post(() -> sizeGridCells(container));
    }

    private void addModule(GridLayout grid, int icon, String name, Class<?> activity) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(icon);
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
