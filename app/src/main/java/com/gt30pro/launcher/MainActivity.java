package com.gt30pro.launcher;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Homescreen minimalis — hanya wallpaper (sesuai preview), tanpa ikon/dock/widget.
 *
 * Akses melalui gesture:
 *   - Swipe ke atas  -> laci aplikasi (semua aplikasi)
 *   - Double-tap     -> Super App
 *   - Long-press     -> menu pintasan (Super App / Semua Aplikasi / Setelan)
 */
public class MainActivity extends Activity {

    private GestureDetector detector;
    private TextView batteryLed;
    private View clockLed;
    private View clockSep;
    private TextView clockHourText;
    private TextView clockMinText;
    private ValueAnimator chargeAnim;
    private boolean charging = false;

    // Gradien simetris memberi pusat keemasan dan tepi jingga kemerahan.
    private static final int[] AMBER_LED = {
            0xFFCB633E, 0xFFFFB568, 0xFFFFE5B1, 0xFFFFB568, 0xFFCB633E
    };
    private static final float[] AMBER_STOPS = {0f, 0.24f, 0.5f, 0.76f, 1f};

    private static class AmberClockText extends TextView {
        AmberClockText(Context context) { super(context); }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (h > 0) {
                getPaint().setShader(new android.graphics.LinearGradient(0, 0, 0, h,
                        AMBER_LED, AMBER_STOPS, android.graphics.Shader.TileMode.CLAMP));
            }
        }
    }

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            if (clockHourText != null && clockMinText != null) {
                String t = new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
                clockHourText.setText(t.substring(0, 2));
                clockMinText.setText(t.substring(3, 5));
            }
            clockHandler.postDelayed(this, 1000);
        }
    };

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level >= 0 && scale > 0 && batteryLed != null) {
                batteryLed.setText((level * 100 / scale) + "%");
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                        || intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
                updateChargeAnim();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView wallpaper = findViewById(R.id.wallpaper);
        wallpaper.setImageBitmap(Wallpapers.load(this));

        // Label indikator baterai (font Doto, putih, kecil, agak transparan) di portal USB Type-C
        batteryLed = new TextView(this);
        batteryLed.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf"));
        batteryLed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        batteryLed.setTextColor(0xFFFFFFFF);
        batteryLed.setAlpha(0.6f);
        batteryLed.setShadowLayer(8f, 0f, 0f, 0x8CFFFFFF);
        batteryLed.setLetterSpacing(0.06f);
        batteryLed.setFontVariationSettings("'wght' 700");
        batteryLed.setText("85%");
        FrameLayout.LayoutParams ledParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        ledParams.bottomMargin = (int) (getResources().getDisplayMetrics().heightPixels * 0.012f);
        ((FrameLayout) findViewById(R.id.root)).addView(batteryLed, ledParams);

        // Jam digital 3 baris: jam / titik / menit (font Doto, LED amber)
        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.CENTER_HORIZONTAL);
        clockHourText = new AmberClockText(this);
        clockHourText.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf"));
        clockHourText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
        clockHourText.setTextColor(0xFFE7A579);
        clockHourText.setShadowLayer(10f, 0f, 0f, 0xA6DD6234);
        clockHourText.setLetterSpacing(0.06f);
        clockHourText.setFontVariationSettings("'wght' 700");
        clockHourText.setIncludeFontPadding(false);
        clockHourText.setText("00");
        clockBox.addView(clockHourText);

        float density = getResources().getDisplayMetrics().density;
        clockSep = new View(this) {
            private final android.graphics.Paint dotPaint =
                    new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            {
                dotPaint.setColor(0xFFE7A579);
            }

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                float r = getHeight() / 2f;
                float cy = getHeight() / 2f;
                int n = 5;
                float gap = (getWidth() - 2f * r) / (n - 1);
                for (int i = 0; i < n; i++) {
                    float x = r + i * gap;
                    dotPaint.setShader(new android.graphics.RadialGradient(x, cy, r,
                            new int[]{0xFFFFE5B1, 0xFFFFB568, 0xFFCB633E},
                            new float[]{0f, 0.55f, 1f}, android.graphics.Shader.TileMode.CLAMP));
                    canvas.drawCircle(x, cy, r, dotPaint);
                }
            }
        };
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int) (1.2f * density)));
        sepParams.topMargin = 0;
        sepParams.bottomMargin = (int) (3.2f * density);
        sepParams.leftMargin = -1;
        sepParams.rightMargin = (int) (0.8f * density) * 2;
        clockSep.setLayoutParams(sepParams);
        clockBox.addView(clockSep);

        clockMinText = new AmberClockText(this);
        clockMinText.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf"));
        clockMinText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
        clockMinText.setTextColor(0xFFE7A579);
        clockMinText.setShadowLayer(10f, 0f, 0f, 0xA6DD6234);
        clockMinText.setLetterSpacing(0.06f);
        clockMinText.setFontVariationSettings("'wght' 700");
        clockMinText.setIncludeFontPadding(false);
        clockMinText.setText("00");
        clockBox.addView(clockMinText);

        clockLed = clockBox;
        clockLed.setAlpha(1f);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        clockParams.rightMargin = (int) (getResources().getDisplayMetrics().widthPixels * 0.286f);
        clockParams.bottomMargin = (int) (getResources().getDisplayMetrics().heightPixels * 0.0646f);
        FrameLayout homeRoot = findViewById(R.id.root);
        homeRoot.addView(clockBox, clockParams);
        clockBox.setContentDescription("Buka Jam di Super App");
        clockBox.setOnClickListener(v -> startActivity(new Intent(this, ClockActivity.class)));
        // Tambah area sentuh tanpa mengubah ukuran atau posisi visual jam.
        homeRoot.post(() -> {
            android.graphics.Rect bounds = new android.graphics.Rect();
            clockBox.getHitRect(bounds);
            int minSize = Math.round(48 * getResources().getDisplayMetrics().density);
            int dx = Math.max(0, (minSize - bounds.width() + 1) / 2);
            int dy = Math.max(0, (minSize - bounds.height() + 1) / 2);
            bounds.inset(-dx, -dy);
            homeRoot.setTouchDelegate(new android.view.TouchDelegate(bounds, clockBox));
        });

        View root = findViewById(R.id.root);
        detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                openSuperApp();
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 != null && e2 != null
                        && e1.getY() - e2.getY() > 120
                        && Math.abs(velocityY) > 200) {
                    openDrawer();
                    return true;
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                showQuickMenu();
            }
        });
        root.setOnTouchListener((v, event) -> detector.onTouchEvent(event));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        clockHandler.removeCallbacks(clockTick);
        clockHandler.post(clockTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryReceiver);
        clockHandler.removeCallbacks(clockTick);
        if (chargeAnim != null) {
            chargeAnim.cancel();
            chargeAnim = null;
        }
    }

    /** Denyut glow HANYA pada garis pemisah jam saat perangkat sedang di-charge. */
    private void updateChargeAnim() {
        if (charging && chargeAnim == null) {
            chargeAnim = ValueAnimator.ofFloat(0.15f, 1f);
            chargeAnim.setDuration(900);
            chargeAnim.setRepeatMode(ValueAnimator.REVERSE);
            chargeAnim.setRepeatCount(ValueAnimator.INFINITE);
            chargeAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            chargeAnim.addUpdateListener(a -> {
                if (clockSep != null) clockSep.setAlpha((float) a.getAnimatedValue());
            });
            chargeAnim.start();
        } else if (!charging && chargeAnim != null) {
            chargeAnim.cancel();
            chargeAnim = null;
            if (clockSep != null) clockSep.setAlpha(1f);
        }
    }

    private void openSuperApp() {
        startActivity(new Intent(this, SuperAppActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void openDrawer() {
        startActivity(new Intent(this, AppDrawerActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showQuickMenu() {
        String[] items = {
                getString(R.string.menu_super_app),
                getString(R.string.menu_all_apps),
                getString(R.string.menu_settings),
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        openSuperApp();
                    } else if (which == 1) {
                        openDrawer();
                    } else {
                        try {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }
}
