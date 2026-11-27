package com.gt30pro.launcher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Laci aplikasi: daftar semua aplikasi terpasang + pencarian. */
public class AppDrawerActivity extends SwipeBackActivity {

    private AppGridAdapter adapter;
    private AppUi ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_drawer);

        ui = new AppUi(this); ui.window(this);
        ImageView wallpaper = findViewById(R.id.wallpaper);
        wallpaper.setVisibility(View.GONE);

        View root = findViewById(R.id.root);
        android.widget.LinearLayout content = findViewById(R.id.content);
        root.setBackgroundColor(ui.canvas);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            content.setPadding(dp(20), dp(16) + insets.getSystemWindowInsetTop(),
                    dp(20), dp(24) + insets.getSystemWindowInsetBottom());
            return insets;
        });

        Typeface doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        content.removeView(findViewById(R.id.back)); content.removeView(findViewById(R.id.drawer_title));
        content.addView(ui.header(this, doto, "Semua Aplikasi"),0);
        EditText searchBox = findViewById(R.id.search); searchBox.setTypeface(doto);
        searchBox.setTextColor(ui.ink); searchBox.setHintTextColor(ui.muted); searchBox.setBackground(ui.surface(this, ui.card));

        GridView grid = findViewById(R.id.grid);
        adapter = new AppGridAdapter(this);
        adapter.setTextColor(ui.ink);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> {
            AppInfo info = adapter.getItem(position);
            Intent launch = getPackageManager().getLaunchIntentForPackage(info.packageName);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        EditText search = findViewById(R.id.search);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadApps();
    }

    @Override protected void onResume() { super.onResume(); AppUi current = new AppUi(this); if (ui != null && (ui.dark != current.dark || !ui.choice.equals(current.choice))) recreate(); }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void loadApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            Intent query = new Intent(Intent.ACTION_MAIN);
            query.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> resolveInfos = pm.queryIntentActivities(query, 0);

            List<AppInfo> apps = new ArrayList<>();
            for (ResolveInfo info : resolveInfos) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName())) {
                    continue;
                }
                AppInfo app = new AppInfo();
                app.label = info.loadLabel(pm).toString();
                app.packageName = pkg;
                app.icon = info.loadIcon(pm);
                apps.add(app);
            }

            Collator collator = Collator.getInstance(new Locale("id", "ID"));
            Collections.sort(apps, (a, b) -> collator.compare(a.label, b.label));
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                adapter.setApps(apps);
                EditText search = findViewById(R.id.search);
                adapter.getFilter().filter(search.getText());
            });
        }).start();
    }
}
