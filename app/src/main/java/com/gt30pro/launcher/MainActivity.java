package com.gt30pro.launcher;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
 * Homescreen minimalis — indikator dan wallpaper di balik lapisan kaca transparan.
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
    private BatteryBoltView batteryBolt;
    private BatteryInfoView batteryInfo;
    private Intent lastBattery;
    private final Runnable batteryInfoTick=new Runnable() {
        @Override public void run() {
            if (!homeResumed) return;
            if (batteryInfo!=null && lastBattery!=null) batteryInfo.update(lastBattery);
            clockHandler.postDelayed(this,15000);
        }
    };
    private HomeClockView clockLed;
    private HomeDateView dateLed;
    private WeatherView weatherLed;
    private WeatherController weather;
    private ValueAnimator chargeAnim;
    private ValueAnimator dateAnim;
    private CircuitGlowView amberGlow;
    private ImageView screenGlass;
    private android.widget.ImageButton voiceButton;
    private Boolean glassDark;
    private boolean homeResumed;
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
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            if (level >= 0 && scale > 0 && batteryLed != null) {
                int percent = level * 100 / scale;
                batteryLed.setText(String.valueOf(percent));
                batteryLed.setContentDescription("Baterai " + percent + " persen"
                        + (charging ? ", pengisian daya" : ""));
            }
            lastBattery=intent;
            if (batteryInfo!=null) batteryInfo.update(intent);
            updateChargeAnim();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FrameLayout micRoot=findViewById(R.id.root);
        android.widget.ImageButton mic=VoiceController.button(this,0xFFB7CBD6); voiceButton=mic;
        int micSize=Math.round(48*getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams micParams=new FrameLayout.LayoutParams(micSize,micSize,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        mic.setElevation(4f*getResources().getDisplayMetrics().density); micRoot.addView(mic,micParams);

        ImageView wallpaper = findViewById(R.id.wallpaper);
        wallpaper.setImageBitmap(Wallpapers.load(this));
        amberGlow = findViewById(R.id.amber_glow);
        amberGlow.load();
        amberGlow.setVisibility(View.INVISIBLE);
        screenGlass = findViewById(R.id.screen_glass);
        // Higher Z than every home indicator, without intercepting taps.
        screenGlass.setElevation(8f * getResources().getDisplayMetrics().density);
        updateScreenGlass();

        // Angka baterai Doto; posisi ikon pengisian dihitung terpisah.
        batteryLed = new TextView(this);
        batteryLed.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf"));
        batteryLed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        batteryLed.setTextColor(0xFFAABAC5);
        batteryLed.setAlpha(1f);
        batteryLed.setScaleX(1.215f);
        batteryLed.setScaleY(1.215f);
        batteryLed.setShadowLayer(8f, 0f, 0f, 0x8CAABAC5);
        batteryLed.setLetterSpacing(0.06f);
        batteryLed.setFontVariationSettings("'wght' 700");
        batteryLed.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        batteryLed.setSingleLine(true);
        batteryLed.setText("85");
        batteryLed.setContentDescription("Baterai 85 persen");
        // Three digits fit without changing the right edge as the percentage changes.
        float batteryDigitAdvance = batteryLed.getPaint().measureText("0")
                + batteryLed.getLetterSpacing() * batteryLed.getTextSize();
        int batteryWidth = (int) Math.ceil(3f * batteryDigitAdvance)
                + batteryLed.getPaddingLeft() + batteryLed.getPaddingRight();
        FrameLayout.LayoutParams ledParams = new FrameLayout.LayoutParams(
                batteryWidth, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        ledParams.bottomMargin = (int) (getResources().getDisplayMetrics().heightPixels * 0.012f);
        FrameLayout homeRoot = findViewById(R.id.root);
        homeRoot.addView(batteryLed, ledParams);
        batteryBolt = new BatteryBoltView(this);
        batteryBolt.setVisibility(View.INVISIBLE);
        homeRoot.addView(batteryBolt, new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT));
        // Read-only card is behind the glass, does not consume home gestures.
        batteryInfo=new BatteryInfoView(this);
        batteryInfo.setElevation(4f * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams infoParams=new FrameLayout.LayoutParams(1,FrameLayout.LayoutParams.WRAP_CONTENT,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        homeRoot.addView(batteryInfo,infoParams);
        homeRoot.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,oright,ob) -> {
            if(r-l!=oright-ol || b-t!=ob-ot) updateBatteryInfoLayout(homeRoot);
        });
        homeRoot.post(() -> updateBatteryInfoLayout(homeRoot));
        batteryLed.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (l != oldL || t != oldT || r != oldR || b != oldB) updateBatteryOffset(homeRoot, batteryLed);
        });
        homeRoot.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL || b - t != oldB - oldT) updateBatteryOffset(homeRoot, batteryLed);
        });
        homeRoot.post(() -> updateBatteryOffset(homeRoot, batteryLed));

        // The clock is a single explicitly measured drawing, above the wallpaper.
        HomeClockView clockBox = new HomeClockView(this);
        clockLed = clockBox;
        clockBox.setVisibility(View.VISIBLE);
        clockBox.setAlpha(1f);
        clockBox.setScaleX(1.2f);
        clockBox.setScaleY(1.2f);
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

        // Date remains a separate overlay and opens the same Calendar as Super App.
        HomeDateView dateBox = new HomeDateView(this);
        dateLed = dateBox;
        dateBox.setDate(LocalDate.now());
        dateBox.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        dateBox.setElevation(4f * getResources().getDisplayMetrics().density);
        homeRoot.addView(dateBox, new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT));
        homeRoot.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL || b - t != oldB - oldT) updateDateLayout(homeRoot, dateBox);
        });
        homeRoot.post(() -> updateDateLayout(homeRoot, dateBox));

        WeatherView weatherBox = new WeatherView(this, true);
        weatherLed = weatherBox;
        weatherBox.setScaleX(.9f);
        weatherBox.setScaleY(.9f);
        weatherBox.setElevation(4f * getResources().getDisplayMetrics().density);
        homeRoot.addView(weatherBox, new FrameLayout.LayoutParams(1, 1, Gravity.TOP | Gravity.LEFT));
        weather = new WeatherController(this, weatherBox);
        weatherBox.setOpensDetails(true);
        weatherBox.setContentDescription("Buka Cuaca");
        weatherBox.setOnClickListener(v -> startActivity(new Intent(this, WeatherActivity.class)));
        homeRoot.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL || b - t != oldB - oldT) updateWeatherLayout(homeRoot, weatherBox);
        });
        homeRoot.post(() -> updateWeatherLayout(homeRoot, weatherBox));
        // Reconcile once the complete view hierarchy has been laid out. This also
        // retries if an earlier post ran before the root had a measured size.
        homeRoot.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (homeRoot.getWidth() > 0 && homeRoot.getHeight() > 0) {
                updateDateLayout(homeRoot, dateBox);
                updateWeatherLayout(homeRoot, weatherBox);
            }
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

    private void updateBatteryInfoLayout(FrameLayout root) {
        if(batteryInfo==null || root.getWidth()<=0 || root.getHeight()<=0) return;
        FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)batteryInfo.getLayoutParams();
        float cssPixel=(root.getWidth()/70.9f)*25.4f/96f;
        int width=Math.round(root.getWidth()*.84f), bottom=Math.round(root.getHeight()*.14f+50f*cssPixel);
        if(p.width!=width || p.bottomMargin!=bottom) { p.width=width; p.bottomMargin=bottom; batteryInfo.setLayoutParams(p); }
    }

    // Wallpaper is centered with centerCrop, just like CSS background-size: cover.
    private float wallpaperScale(FrameLayout root) {
        return Math.max(root.getWidth() / 1224f, root.getHeight() / 2720f);
    }

    private void updateBatteryOffset(FrameLayout root, TextView batteryBox) {
        if (root.getWidth() <= 0) return;
        float pixelsPerMm = root.getWidth() / 70.9f;
        float cssPixel = pixelsPerMm * 25.4f / 96f;
        // Scale from the right edge so resizing never moves the aligned edge.
        batteryBox.setPivotX(batteryBox.getWidth());
        batteryBox.setTranslationX(cssPixel + 495f * wallpaperScale(root));
        batteryBox.setTranslationY(cssPixel - 1470f * wallpaperScale(root));
        if (batteryBolt == null || batteryBox.getBottom() <= 0) return;
        int boltWidth = Math.max(1, Math.round(BatteryBoltView.WIDTH_MM * pixelsPerMm));
        int boltHeight = Math.max(1, Math.round(BatteryBoltView.HEIGHT_MM * pixelsPerMm));
        float left = root.getWidth() / 2f + cssPixel - boltWidth / 2f;
        // Position against the unshifted text layout, independently of the number.
        // The last 2 CSS pixels are the additional user-requested upward nudge.
        float top = batteryBox.getBottom() - 2.75f * cssPixel - 2f * pixelsPerMm;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) batteryBolt.getLayoutParams();
        params.width = boltWidth; params.height = boltHeight;
        params.leftMargin = Math.round(left); params.topMargin = Math.round(top);
        batteryBolt.setTranslationX(left - params.leftMargin);
        batteryBolt.setTranslationY(top - params.topMargin);
        batteryBolt.setLayoutParams(params);
        // Anchor to the bolt's untransformed position, not the shifted battery percentage.
        // Shift the voice button 41 preview pixels left and 11 up; keep it available
        // even when the charging bolt is hidden.
        if (voiceButton != null) {
            FrameLayout.LayoutParams voiceParams=(FrameLayout.LayoutParams) voiceButton.getLayoutParams();
            int margin=Math.max(0,Math.round(root.getHeight() - top + pixelsPerMm + 11f * cssPixel));
            if (voiceParams.bottomMargin != margin) { voiceParams.bottomMargin=margin; voiceButton.setLayoutParams(voiceParams); }
            voiceButton.setTranslationX(-40f * cssPixel);
        }
    }

    private void updateClockLayout(FrameLayout root, HomeClockView clockBox) {
        int width = root.getWidth(), height = root.getHeight();
        if (width <= 0 || height <= 0) return;
        float pixelsPerMm = width / 70.9f;
        float cssPixel = pixelsPerMm * 25.4f / 96f;
        int boxWidth = Math.max(1, Math.round(HomeClockView.WIDTH_MM * pixelsPerMm));
        int boxHeight = Math.max(1, Math.round(HomeClockView.HEIGHT_MM * pixelsPerMm));
        // Keep the content (not its glow padding) anchored to the preview position.
        float left = width * .357f - .5f * cssPixel - boxWidth / 2f
                + 750f * wallpaperScale(root);
        float bottom = height * .06f + .55f * cssPixel;
        float top = height - bottom - boxHeight + HomeClockView.GLOW_PAD_MM * pixelsPerMm;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) clockBox.getLayoutParams();
        params.width = boxWidth;
        params.height = boxHeight;
        params.leftMargin = Math.round(left);
        params.topMargin = Math.round(top);
        clockBox.setTranslationX(left - params.leftMargin);
        clockBox.setTranslationY(top - params.topMargin - 2182f * wallpaperScale(root));
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
        // Match the preview offsets (1 CSS px = 25.4/96 mm).
        float left = width * .49f + 115f * cssPixel - boxWidth / 2f;
        float bottom = height * .06f + 384.55f * cssPixel;
        float top = height - bottom - boxHeight + HomeDateView.GLOW_PAD_MM * pixelsPerMm;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) dateBox.getLayoutParams();
        int leftMargin = Math.round(left), topMargin = Math.round(top);
        boolean changed = params.width != boxWidth || params.height != boxHeight
                || params.leftMargin != leftMargin || params.topMargin != topMargin;
        params.width = boxWidth; params.height = boxHeight;
        params.leftMargin = leftMargin; params.topMargin = topMargin;
        dateBox.setTranslationX(left - leftMargin);
        dateBox.setTranslationY(top - topMargin);
        dateBox.setScale(pixelsPerMm);
        if (changed) dateBox.setLayoutParams(params);
    }

    private void updateWeatherLayout(FrameLayout root, WeatherView box) {
        int width = root.getWidth(), height = root.getHeight();
        if (width <= 0 || height <= 0) return;
        float ppm = width / 70.9f, cssPixel = ppm * 25.4f / 96f;
        int w = Math.max(1, Math.round(11f * ppm));
        int h = Math.max(1, Math.round(13.75f * ppm));
        float left = width * .49f - 2f * cssPixel - w / 2f;
        float top = height - (height * .06f + 460f * cssPixel) - h;
        box.setHomeCssPixel(cssPixel);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) box.getLayoutParams();
        int leftMargin = Math.round(left), topMargin = Math.round(top);
        boolean changed = params.width != w || params.height != h
                || params.leftMargin != leftMargin || params.topMargin != topMargin;
        params.width = w; params.height = h; params.leftMargin = leftMargin; params.topMargin = topMargin;
        box.setTranslationX(left - leftMargin); box.setTranslationY(top - topMargin);
        if (changed) box.setLayoutParams(params);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (weather != null) weather.permissionResult(requestCode);
    }

    private void updateScreenGlass() {
        if (screenGlass == null) return;
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        if (glassDark == null || glassDark.booleanValue() != dark) {
            Bitmap bitmap = Wallpapers.loadScreenGlass(this, dark);
            if (bitmap != null) {
                screenGlass.setImageBitmap(bitmap);
                glassDark = dark;
            }
        }
        if (voiceButton != null) VoiceController.setButtonColor(voiceButton,0xFFFFCD6A);
        if (clockLed != null) clockLed.setLightGlass(!dark);
        if (dateLed != null) dateLed.setLightGlass(!dark);
        if (weatherLed != null) weatherLed.setLightGlass(!dark);
        if (batteryInfo != null) batteryInfo.setLightGlass(!dark);
        if (homeResumed) updateChargeAnim();
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateScreenGlass();
    }

    @Override
    protected void onResume() {
        super.onResume();
        homeResumed = true;
        updateScreenGlass();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(powerSaveReceiver, new IntentFilter(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        updateAmberGlow();
        if (weather != null) weather.resume();
        if (clockLed != null) {
            clockLed.setVisibility(View.VISIBLE);
            clockLed.setAlpha(1f);
            clockLed.bringToFront();
        }
        if (dateLed != null) {
            dateLed.setVisibility(View.VISIBLE);
            dateLed.setAlpha(.85f);
            dateLed.bringToFront();
            startDateAnimation();
        }
        if (weatherLed != null) {
            weatherLed.setVisibility(View.VISIBLE);
            weatherLed.setAlpha(1f);
            weatherLed.bringToFront();
        }
        FrameLayout homeRoot = findViewById(R.id.root);
        homeRoot.post(() -> {
            if (dateLed != null) updateDateLayout(homeRoot, dateLed);
            if (weatherLed != null) updateWeatherLayout(homeRoot, weatherLed);
        });
        clockHandler.removeCallbacks(clockTick);
        clockHandler.post(clockTick);
        clockHandler.removeCallbacks(batteryInfoTick);
        clockHandler.post(batteryInfoTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        homeResumed = false;
        unregisterReceiver(batteryReceiver);
        unregisterReceiver(powerSaveReceiver);
        stopAmberGlow();
        if (weather != null) weather.pause();
        stopDateAnimation();
        clockHandler.removeCallbacks(clockTick);
        clockHandler.removeCallbacks(batteryInfoTick);
        lastBattery=null;
        if (chargeAnim != null) {
            chargeAnim.cancel();
            chargeAnim = null;
        }
        if (batteryLed != null) batteryLed.setAlpha(1f);
        if (batteryBolt != null) {
            batteryBolt.setAlpha(1f);
            batteryBolt.setVisibility(View.INVISIBLE);
        }
    }

    private void startDateAnimation() {
        if (dateLed == null || dateAnim != null || !ValueAnimator.areAnimatorsEnabled()) return;
        dateAnim = ValueAnimator.ofFloat(.8f, 1f);
        dateAnim.setDuration(2000);
        dateAnim.setRepeatMode(ValueAnimator.REVERSE);
        dateAnim.setRepeatCount(ValueAnimator.INFINITE);
        dateAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        dateAnim.addUpdateListener(a -> {
            if (dateLed != null) dateLed.setAlpha((float) a.getAnimatedValue());
        });
        dateAnim.start();
    }

    private void stopDateAnimation() {
        if (dateAnim != null) {
            dateAnim.cancel();
            dateAnim = null;
        }
        if (dateLed != null) dateLed.setAlpha(.85f);
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

    /** Pulse only the battery indicator and its dotted bolt while connected to power. */
    private void updateChargeAnim() {
        boolean light = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != Configuration.UI_MODE_NIGHT_YES;
        if (batteryLed != null) {
            // Preserve the existing gunmetal brightness when not charging; the
            // amber charging color matches the animation without a color filter.
            android.graphics.Paint brightness = null;
            if (!charging) {
                float factor = light ? .55f : 1.4f;
                brightness = new android.graphics.Paint();
                brightness.setColorFilter(new android.graphics.ColorMatrixColorFilter(new float[] {
                        factor, 0, 0, 0, 0, 0, factor, 0, 0, 0,
                        0, 0, factor, 0, 0, 0, 0, 0, 1, 0
                }));
            }
            batteryLed.setLayerType(View.LAYER_TYPE_SOFTWARE, brightness);
        }
        if (charging) {
            if (batteryLed != null) {
                batteryLed.setTextColor(0xFFFFCD6A);
                batteryLed.setShadowLayer(8f, 0f, 0f, 0x8CFF6919);
            }
            if (batteryBolt != null) {
                batteryBolt.setColor(0xFFFFCD6A);
                batteryBolt.setVisibility(View.VISIBLE);
            }
            if (chargeAnim == null) {
                chargeAnim = ValueAnimator.ofFloat(.9f, 1f);
                chargeAnim.setDuration(900);
                chargeAnim.setRepeatMode(ValueAnimator.REVERSE);
                chargeAnim.setRepeatCount(ValueAnimator.INFINITE);
                chargeAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                chargeAnim.addUpdateListener(a -> {
                    float alpha = (float) a.getAnimatedValue();
                    if (batteryLed != null) batteryLed.setAlpha(alpha);
                    if (batteryBolt != null) batteryBolt.setAlpha(alpha);
                });
                chargeAnim.start();
            }
        } else {
            if (chargeAnim != null) {
                chargeAnim.cancel();
                chargeAnim = null;
            }
            if (batteryLed != null) {
                batteryLed.setTextColor(light ? 0xFF263840 : 0xFFAABAC5);
                batteryLed.setShadowLayer(8f, 0f, 0f, light ? 0x44263840 : 0x8CAABAC5);
                batteryLed.setAlpha(1f);
            }
            if (batteryBolt != null) {
                batteryBolt.setColor(light ? 0xFF263840 : 0xFFAABAC5);
                batteryBolt.setAlpha(1f);
                batteryBolt.setVisibility(View.INVISIBLE);
            }
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
                getString(R.string.menu_weather),
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
                    } else if (which == 4 && weather != null) {
                        weather.requestFromUser();
                    }
                })
                .show();
    }
}
