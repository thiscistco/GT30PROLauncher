package com.gt30pro.launcher;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
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
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.time.LocalDate;
import java.time.ZoneId;
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
public class MainActivity extends BottomSwipeActivity {
    @Override protected boolean supportsBottomSwipe() { return true; }
    @Override protected void onBottomSwipe() { openDrawer(); }

    private GestureDetector detector;
    private TextView batteryLed;
    private HomeClockView clockLed;
    private HomeDateView dateLed;
    private ValueAnimator chargeAnim;
    private CircuitGlowView amberGlow;
    private ValueAnimator amberAnim;
    private double amberPhase, amberDurationMs;
    private long amberLastFrame, amberNextMemorySample;
    private static final long MEMORY_SAMPLE_INTERVAL_MS = 3000;
    private boolean charging = false;
    private final BroadcastReceiver powerSaveReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { updateAmberGlow(); }
    };

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            if (clockLed != null) {
                String t = new SimpleDateFormat("HH:mm", Locale.US).format(now);
                clockLed.setTime(t.substring(0, 2), t.substring(3, 5));
            }
            if (dateLed != null) dateLed.setDate(now.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
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
        amberGlow = findViewById(R.id.amber_glow);
        amberGlow.load();
        amberGlow.setVisibility(View.INVISIBLE);

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

        // The clock is a single explicitly measured drawing, above the wallpaper.
        FrameLayout homeRoot = findViewById(R.id.root);
        HomeClockView clockBox = new HomeClockView(this);
        clockLed = clockBox;
        clockBox.setVisibility(View.VISIBLE);
        clockBox.setAlpha(1f);
        clockBox.setElevation(4f * getResources().getDisplayMetrics().density);
        homeRoot.setClipChildren(false);
        homeRoot.addView(clockBox, new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT));
        clockBox.bringToFront();
        clockBox.setContentDescription("Buka Jam di Super App");
        clockBox.setOnClickListener(v -> startActivity(new Intent(this, ClockActivity.class)));
        homeRoot.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL || b - t != oldB - oldT) {
                updateClockLayout(homeRoot, clockBox);
            }
        });
        homeRoot.post(() -> updateClockLayout(homeRoot, clockBox));

        // Date is a separate non-interactive overlay, leaving clock touch/gestures unchanged.
        HomeDateView dateBox = new HomeDateView(this);
        dateLed = dateBox;
        dateBox.setDate(LocalDate.now());
        dateBox.setElevation(4f * getResources().getDisplayMetrics().density);
        homeRoot.addView(dateBox, new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT));
        homeRoot.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL || b - t != oldB - oldT) updateDateLayout(homeRoot, dateBox);
        });
        homeRoot.post(() -> updateDateLayout(homeRoot, dateBox));

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

    private void updateClockLayout(FrameLayout root, HomeClockView clockBox) {
        int width = root.getWidth(), height = root.getHeight();
        if (width <= 0 || height <= 0) return;
        float pixelsPerMm = width / 70.9f;
        float cssPixel = pixelsPerMm * 25.4f / 96f;
        int boxWidth = Math.max(1, Math.round(HomeClockView.WIDTH_MM * pixelsPerMm));
        int boxHeight = Math.max(1, Math.round(HomeClockView.HEIGHT_MM * pixelsPerMm));
        // Keep the content (not its glow padding) anchored to the preview position.
        float left = width * .357f - .5f * cssPixel - boxWidth / 2f;
        float bottom = height * .06f + 1.55f * cssPixel;
        float top = height - bottom - boxHeight + HomeClockView.GLOW_PAD_MM * pixelsPerMm;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) clockBox.getLayoutParams();
        params.width = boxWidth;
        params.height = boxHeight;
        params.leftMargin = Math.round(left);
        params.topMargin = Math.round(top);
        clockBox.setTranslationX(left - params.leftMargin);
        clockBox.setTranslationY(top - params.topMargin);
        clockBox.setScale(pixelsPerMm);
        clockBox.setLayoutParams(params);
        clockBox.post(() -> {
            android.graphics.Rect bounds = new android.graphics.Rect();
            clockBox.getHitRect(bounds);
            int minSize = Math.round(48 * getResources().getDisplayMetrics().density);
            int dx = Math.max(0, (minSize - bounds.width() + 1) / 2);
            int dy = Math.max(0, (minSize - bounds.height() + 1) / 2);
            bounds.inset(-dx, -dy);
            root.setTouchDelegate(new android.view.TouchDelegate(bounds, clockBox));
        });
    }

    private void updateDateLayout(FrameLayout root, HomeDateView dateBox) {
        int width = root.getWidth(), height = root.getHeight();
        if (width <= 0 || height <= 0) return;
        float pixelsPerMm = width / 70.9f;
        float cssPixel = pixelsPerMm * 25.4f / 96f;
        int boxWidth = Math.max(1, Math.round(HomeDateView.WIDTH_MM * pixelsPerMm));
        int boxHeight = Math.max(1, Math.round(HomeDateView.HEIGHT_MM * pixelsPerMm));
        float left = width * .49f - boxWidth / 2f;
        // Match the preview's total 500 CSS px upward shift (1 CSS px = 25.4/96 mm).
        float bottom = height * .06f + 501.55f * cssPixel;
        float top = height - bottom - boxHeight + HomeDateView.GLOW_PAD_MM * pixelsPerMm;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) dateBox.getLayoutParams();
        params.width = boxWidth; params.height = boxHeight;
        params.leftMargin = Math.round(left); params.topMargin = Math.round(top);
        dateBox.setTranslationX(left - params.leftMargin);
        dateBox.setTranslationY(top - params.topMargin);
        dateBox.setScale(pixelsPerMm);
        dateBox.setLayoutParams(params);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(powerSaveReceiver, new IntentFilter(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        updateAmberGlow();
        if (clockLed != null) {
            clockLed.setVisibility(View.VISIBLE);
            clockLed.setAlpha(1f);
            clockLed.bringToFront();
        }
        if (dateLed != null) {
            dateLed.setVisibility(View.VISIBLE);
            dateLed.setAlpha(1f);
            dateLed.bringToFront();
        }
        clockHandler.removeCallbacks(clockTick);
        clockHandler.post(clockTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(batteryReceiver);
        unregisterReceiver(powerSaveReceiver);
        stopAmberGlow();
        clockHandler.removeCallbacks(clockTick);
        if (chargeAnim != null) {
            chargeAnim.cancel();
            chargeAnim = null;
        }
    }

    private boolean isAmberGlowEnabled() {
        return getSharedPreferences("launcher_effects", MODE_PRIVATE).getBoolean("amber_glow", true);
    }

    private void stopAmberGlow() {
        if (amberAnim != null) { amberAnim.cancel(); amberAnim = null; }
        if (amberGlow != null) amberGlow.setVisibility(View.INVISIBLE);
    }

    private double readUsedRamFraction() {
        try {
            ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (manager == null) return 0.5;
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            manager.getMemoryInfo(memory);
            return CircuitMotion.usedRamFraction(memory.totalMem, memory.availMem);
        } catch (RuntimeException ignored) {
            // Some devices may not expose memory info: use the original speed.
            return 0.5;
        }
    }

    private void updateAmberGlow() {
        android.os.PowerManager power = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        if (amberGlow == null || !amberGlow.isReady() || !isAmberGlowEnabled()
                || (power != null && power.isPowerSaveMode()) || !ValueAnimator.areAnimatorsEnabled()) {
            stopAmberGlow();
            return;
        }
        if (amberAnim != null) return;
        amberGlow.setVisibility(View.VISIBLE);
        // The animator only schedules frames. Integrating elapsed time preserves
        // position when memory changes instead of restarting or jumping the light.
        amberLastFrame = SystemClock.uptimeMillis();
        amberDurationMs = CircuitMotion.durationMs(amberGlow.getDurationMs(), readUsedRamFraction());
        amberNextMemorySample = amberLastFrame + MEMORY_SAMPLE_INTERVAL_MS;
        amberGlow.setPhase((float) amberPhase);
        amberAnim = ValueAnimator.ofFloat(0f, 1f);
        amberAnim.setDuration(1000);
        amberAnim.setRepeatMode(ValueAnimator.RESTART);
        amberAnim.setRepeatCount(ValueAnimator.INFINITE);
        amberAnim.setInterpolator(new android.view.animation.LinearInterpolator());
        amberAnim.addUpdateListener(a -> {
            long now = SystemClock.uptimeMillis();
            amberPhase = CircuitMotion.advance(amberPhase, Math.max(0, now - amberLastFrame), amberDurationMs);
            amberLastFrame = now;
            if (now >= amberNextMemorySample) {
                amberDurationMs = CircuitMotion.durationMs(amberGlow.getDurationMs(), readUsedRamFraction());
                amberNextMemorySample = now + MEMORY_SAMPLE_INTERVAL_MS;
            }
            amberGlow.setPhase((float) amberPhase);
        });
        amberAnim.start();
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
                if (clockLed != null) clockLed.setSeparatorAlpha((float) a.getAnimatedValue());
            });
            chargeAnim.start();
        } else if (!charging && chargeAnim != null) {
            chargeAnim.cancel();
            chargeAnim = null;
            if (clockLed != null) clockLed.setSeparatorAlpha(1f);
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
                getString(isAmberGlowEnabled() ? R.string.menu_amber_off : R.string.menu_amber_on),
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        openSuperApp();
                    } else if (which == 1) {
                        openDrawer();
                    } else if (which == 2) {
                        try {
                            startActivity(new Intent(Settings.ACTION_SETTINGS));
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 3) {
                        getSharedPreferences("launcher_effects", MODE_PRIVATE).edit()
                                .putBoolean("amber_glow", !isAmberGlowEnabled()).apply();
                        updateAmberGlow();
                    }
                })
                .show();
    }
}
