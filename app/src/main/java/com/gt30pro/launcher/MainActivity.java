package com.gt30pro.launcher;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Homescreen utama launcher: wallpaper, jam, pintasan favorit,
 * kartu Super App, dan dock.
 */
public class MainActivity extends Activity {

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Locale localeId = new Locale("id", "ID");

    private TextView clockView;
    private TextView dateView;

    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 15000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clockView = findViewById(R.id.clock);
        dateView = findViewById(R.id.date);

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

        // Super App
        View.OnClickListener openSuperApp = v ->
                startActivity(new Intent(MainActivity.this, SuperAppActivity.class));
        findViewById(R.id.hero_super_app).setOnClickListener(openSuperApp);
        findViewById(R.id.dock_super_app).setOnClickListener(openSuperApp);

        // App drawer
        View.OnClickListener openDrawer = v -> openDrawer();
        findViewById(R.id.search_pill).setOnClickListener(openDrawer);
        findViewById(R.id.dock_all_apps).setOnClickListener(openDrawer);

        // Setelan sistem
        findViewById(R.id.dock_settings).setOnClickListener(v ->
                safeStart(new Intent(Settings.ACTION_SETTINGS)));

        // Favorit
        findViewById(R.id.fav_phone).setOnClickListener(v ->
                safeStart(new Intent(Intent.ACTION_DIAL)));

        findViewById(R.id.fav_messages).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
            if (!startByCategory(intent,
                    new String[]{"com.android.mms",
                            "com.google.android.apps.messaging",
                            "com.samsung.android.messaging"})) {
                toastNotFound();
            }
        });

        findViewById(R.id.fav_camera).setOnClickListener(v ->
                safeStart(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));

        findViewById(R.id.fav_browser).setOnClickListener(v -> {
            if (!startByPackage(new String[]{"com.android.chrome",
                    "com.android.browser",
                    "org.mozilla.firefox",
                    "com.brave.browser"})) {
                safeStart(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")));
            }
        });
    }

    private void openDrawer() {
        startActivity(new Intent(this, AppDrawerActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void safeStart(Intent intent) {
        try {
            startActivity(intent);
        } catch (Exception e) {
            toastNotFound();
        }
    }

    private boolean startByPackage(String[] packages) {
        for (String pkg : packages) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);
                return true;
            }
        }
        return false;
    }

    private boolean startByCategory(Intent intent, String[] fallbackPackages) {
        try {
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return startByPackage(fallbackPackages);
        }
    }

    private void toastNotFound() {
        Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show();
    }

    private void updateClock() {
        Date now = new Date();
        clockView.setText(new SimpleDateFormat("HH:mm", localeId).format(now));
        dateView.setText(new SimpleDateFormat("EEEE, d MMMM yyyy", localeId).format(now));
    }

    @Override
    protected void onResume() {
        super.onResume();
        clockHandler.removeCallbacks(clockTick);
        clockHandler.post(clockTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        clockHandler.removeCallbacks(clockTick);
    }
}
