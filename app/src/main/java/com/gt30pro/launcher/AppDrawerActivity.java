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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_drawer);

        ImageView wallpaper = findViewById(R.id.wallpaper);
        wallpaper.setImageBitmap(Wallpapers.load(this));

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
        ((TextView) findViewById(R.id.drawer_title)).setTypeface(doto);
        ((EditText) findViewById(R.id.search)).setTypeface(doto);
        findViewById(R.id.back).setOnClickListener(v -> finish());

        GridView grid = findViewById(R.id.grid);
        adapter = new AppGridAdapter(this);
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
            runOnUiThread(() -> adapter.setApps(apps));
        }).start();
    }
}
