package com.gt30pro.launcher;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Pusat aplikasi yang dibuat di dalam launcher. */
public class SuperAppActivity extends SwipeBackActivity {

    private static final String[][] MODULES = {
            {"⏱️", "Jam", "Waktu & tanggal"},
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_app);

        getWindow().setStatusBarColor(android.graphics.Color.BLACK);
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);

        View root = findViewById(R.id.root);
        View content = findViewById(R.id.content);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            content.setPadding(content.getPaddingLeft(),
                    insets.getSystemWindowInsetTop(),
                    content.getPaddingRight(),
                    insets.getSystemWindowInsetBottom());
            return insets;
        });

        Typeface doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        ((TextView) findViewById(R.id.back)).setTypeface(doto);
        ((TextView) findViewById(R.id.super_title)).setTypeface(doto);
        ((TextView) findViewById(R.id.super_subtitle)).setTypeface(doto);
        findViewById(R.id.back).setOnClickListener(v -> finish());

        LinearLayout container = findViewById(R.id.modules_container);
        LinearLayout row = null;
        for (int i = 0; i < MODULES.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                if (i > 0) {
                    rowParams.topMargin = dp(12);
                }
                container.addView(row, rowParams);
            }
            row.addView(buildCard(MODULES[i][0], MODULES[i][1], MODULES[i][2]), cardParams(i % 2));
        }
    }

    private LinearLayout.LayoutParams cardParams(int column) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (column == 1) {
            params.leftMargin = dp(12);
        }
        return params;
    }

    private View buildCard(String emoji, String title, String subtitle) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundResource(R.drawable.bg_module_card);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setGravity(Gravity.CENTER_VERTICAL);

        TextView tile = new TextView(this);
        tile.setText(emoji);
        tile.setTextSize(22);
        tile.setGravity(Gravity.CENTER);
        tile.setBackgroundResource(R.drawable.bg_module_tile);
        card.addView(tile, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textsParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textsParams.leftMargin = dp(12);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(15);
        titleView.setTypeface("Jam".equals(title)
                ? Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf") : Typeface.DEFAULT_BOLD);
        titleView.setTextColor(getResources().getColor(R.color.text_primary));
        texts.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf"));
        subtitleView.setTextSize(11);
        subtitleView.setTextColor(getResources().getColor(R.color.text_secondary));
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(2);
        texts.addView(subtitleView, subtitleParams);

        card.addView(texts, textsParams);
        card.setOnClickListener(v -> {
            if ("Jam".equals(title)) {
                startActivity(new Intent(this, ClockActivity.class));
            } else {
                Toast.makeText(this, getString(R.string.coming_soon, title), Toast.LENGTH_SHORT).show();
            }
        });
        return card;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
