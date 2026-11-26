package com.gt30pro.launcher;

import android.animation.ValueAnimator;
import android.app.Activity;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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
    private CircuitGlowView amberGlow;
    private ValueAnimator amberAnim;
    private double amberPhase, amberDurationMs;
    private long amberLastFrame, amberNextMemorySample;
    private static final long MEMORY_SAMPLE_INTERVAL_MS = 3000;
    private boolean charging = false;
    private final BroadcastReceiver powerSaveReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { updateAmberGlow(); }
    };

    // Gradien simetris memberi pusat keemasan dan tepi jingga kemerahan.
    private static final int[] AMBER_LED = {
            0xFFCB633E, 0xFFFFB568, 0xFFFFE5B1, 0xFFFFB568, 0xFFCB633E
    };
    private static final float[] AMBER_STOPS = {0f, 0.24f, 0.5f, 0.76f, 1f};

    private static class AmberClockText extends TextView {
        private android.graphics.Shader gradient;
        AmberClockText(Context context) { super(context); }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            if (h > 0) gradient = new android.graphics.LinearGradient(0, 0, 0, h,
                    AMBER_LED, AMBER_STOPS, android.graphics.Shader.TileMode.CLAMP);
        }

        @Override protected void onDraw(android.graphics.Canvas canvas) {
            if (gradient != null) getPaint().setShader(gradient);
            super.onDraw(canvas);
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

        // Jam digital 3 baris: jam / titik / menit (font Doto, LED amber)
        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.CENTER_HORIZONTAL);
        clockHourText = new AmberClockText(this);
        clockHourText.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf"));
        clockHourText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
        clockHourText.setTextColor(0xFFE7A579);
        clockHourText.setLetterSpacing(0.06f);
        clockHourText.setFontVariationSettings("'wght' 800");
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
        clockMinText.setLetterSpacing(0.06f);
        clockMinText.setFontVariationSettings("'wght' 800");
        clockMinText.setIncludeFontPadding(false);
        clockMinText.setText("00");
        clockBox.addView(clockMinText);

        clockLed = clockBox;
        clockLed.setAlpha(1f);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        clockParams.rightMargin = (int) (getResources().getDisplayMetrics().widthPixels * 0.143f);
        clockParams.bottomMargin = (int) (getResources().getDisplayMetrics().heightPixels * 0.0646f);
        FrameLayout homeRoot = findViewById(R.id.root);
        homeRoot.addView(clockBox, clockParams);
        clockBox.setContentDescription("Buka Jam di Super App");
        clockBox.setOnClickListener(v -> startActivity(new Intent(this, ClockActivity.class)));
        // Ukuran dan posisi mengacu pada area wallpaper yang benar-benar terukur,
        // bukan ukuran layar yang dapat mencakup status/navigation bar.
        homeRoot.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL || b - t != oldB - oldT) {
                updateClockLayout(homeRoot, clockBox);
            }
        });
        homeRoot.post(() -> updateClockLayout(homeRoot, clockBox));

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

    private void updateClockLayout(FrameLayout root, LinearLayout clockBox) {
        int width = root.getWidth(), height = root.getHeight();
        if (width <= 0 || height <= 0) return;
        // Preview screen: 70.9mm wide. Match its 2mm font and CSS-pixel offsets.
        float cssPixel = width / (70.9f * 96f / 25.4f);
        float clockSizePx = width * (2f / 70.9f);
        clockHourText.setTextSize(TypedValue.COMPLEX_UNIT_PX, clockSizePx);
        clockMinText.setTextSize(TypedValue.COMPLEX_UNIT_PX, clockSizePx);
        // Match the tighter preview glow to keep the small Doto dots legible.
        float clockGlowRadius = width * (0.4f / 70.9f);
        clockHourText.setShadowLayer(clockGlowRadius, 0f, 0f, 0xA6DD6234);
        clockMinText.setShadowLayer(clockGlowRadius, 0f, 0f, 0xA6DD6234);
        clockHourText.setLetterSpacing(0.2f / 2f);
        clockMinText.setLetterSpacing(0.2f / 2f);
        LinearLayout.LayoutParams sep = (LinearLayout.LayoutParams) clockSep.getLayoutParams();
        sep.height = Math.max(1, Math.round(width * (0.22f / 70.9f)));
        sep.topMargin = Math.max(0, Math.round((0.461f * 96f / 25.4f - 1f) * cssPixel));
        sep.bottomMargin = Math.max(0, Math.round((0.45f * 96f / 25.4f + 1f) * cssPixel));
        sep.leftMargin = Math.round(-0.25f * cssPixel);
        sep.rightMargin = Math.round(1.01f * cssPixel);
        clockSep.setLayoutParams(sep);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) clockBox.getLayoutParams();
        // FrameLayout subtracts the full right margin from CENTER_HORIZONTAL.
        // 50% - 14.3% places the clock at the preview's 35.7% center.
        params.rightMargin = Math.round(width * 0.143f);
        params.bottomMargin = Math.round(height * 0.06f + 3f * cssPixel);
        // Apply subpixel shifts in preview CSS pixels (1 CSS px = cssPixel).
        clockBox.setTranslationX(-0.5f * cssPixel);
        clockBox.setTranslationY(1.45f * cssPixel);
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

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(powerSaveReceiver, new IntentFilter(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        updateAmberGlow();
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
