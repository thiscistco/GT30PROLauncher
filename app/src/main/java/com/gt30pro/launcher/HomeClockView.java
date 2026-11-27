package com.gt30pro.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Explicitly sized home clock with a five-dot accumulating gunmetal animation. */
final class HomeClockView extends View {
    static final float WIDTH_MM = 3.6f;
    static final float CONTENT_HEIGHT_MM = 5.731f;
    static final float GLOW_PAD_MM = 0.4f;
    static final float HEIGHT_MM = CONTENT_HEIGHT_MM + 2f * GLOW_PAD_MM;
    private static final int[] AMBER = {
        0xFFFFCD6A, 0xFFFFCD6A, 0xFFFFCD6A, 0xFFFFCD6A, 0xFFFFCD6A
    };
    private static final int[] AMBER_LIGHT = {0xFFFFCD6A, 0xFFFFCD6A, 0xFFFFCD6A, 0xFFFFCD6A, 0xFFFFCD6A};
    private boolean lightGlass;
    private static final float[] STOPS = {0f, .24f, .5f, .76f, 1f};
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String hour = "00", minute = "00";
    private float pxPerMm = 1f;
    private ValueAnimator separatorAnimator;
    private float separatorPhase = -1f;
    private static final float HOME_SCALE = 1.14f;

    void startSeparator() {
        stopSeparator();
        if (!ValueAnimator.areAnimatorsEnabled()) return;
        separatorAnimator = ValueAnimator.ofFloat(0f, 6f);
        separatorAnimator.setDuration(6000);
        separatorAnimator.setInterpolator(new LinearInterpolator());
        separatorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        separatorAnimator.addUpdateListener(animation -> {
            separatorPhase = (Float) animation.getAnimatedValue();
            invalidate();
        });
        separatorAnimator.start();
    }
    void stopSeparator() {
        if (separatorAnimator != null) { separatorAnimator.cancel(); separatorAnimator = null; }
        separatorPhase = -1f;
        invalidate();
    }

    HomeClockView(Context context) {
        super(context);
        // Render this small overlay independently of device-specific GPU text effects.
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setWillNotDraw(false);
        text.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Doto.ttf"));
        text.setFontVariationSettings("'wght' 800");
        text.setColor(0xFFFFCD6A);
    }

    void setScale(float pixelsPerMm) {
        pxPerMm = pixelsPerMm;
        text.setTextSize(2f * pixelsPerMm);
        text.setShadowLayer(.4f * pixelsPerMm, 0, 0, 0xA6FF6919);
        invalidate();
    }

    void setLightGlass(boolean light) {
        if (lightGlass != light) { lightGlass = light; invalidate(); }
    }

    void setTime(String hours, String minutes) {
        if (!hour.equals(hours) || !minute.equals(minutes)) {
            hour = hours;
            minute = minutes;
            invalidate();
        }
    }

    private void drawDigits(Canvas canvas, String value, float top, float lineHeight) {
        Paint.FontMetrics metrics = text.getFontMetrics();
        float baseline = top + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent;
        float gap = .2f * pxPerMm;
        float firstWidth = text.measureText(value, 0, 1);
        float secondWidth = text.measureText(value, 1, 2);
        float x = (getWidth() - firstWidth - gap - secondWidth) / 2f;
        text.setShadowLayer(.4f * pxPerMm, 0, 0, 0xA6FF6919);
        text.setShader(new LinearGradient(0, top, 0, top + lineHeight, lightGlass ? AMBER_LIGHT : AMBER, STOPS, Shader.TileMode.CLAMP));
        canvas.drawText(value, 0, 1, x, baseline, text);
        canvas.drawText(value, 1, 2, x + firstWidth + gap, baseline, text);
        text.setShader(null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float line = 2.3f * pxPerMm;
        float top = GLOW_PAD_MM * pxPerMm;
        drawDigits(canvas, hour, top, line);
        float cssPx = pxPerMm * 25.4f / 96f;
        // Preserve both digit rows: center the dots, then raise only the separator by half a final CSS pixel.
        float originalDotTop = top + line + .461f * pxPerMm - cssPx;
        float minuteTop = originalDotTop + .22f * pxPerMm + .45f * pxPerMm;
        float r = .14f * pxPerMm;
        float dotTop = (top + line + minuteTop - cssPx / HOME_SCALE) / 2f - r - .5f * cssPx / HOME_SCALE;
        float total = 2.8f * pxPerMm - .76f * cssPx;
        float center = getWidth() / 2f - .63f * cssPx;
        float start = center - total / 2f + total / 10f;
        dots.setColor(lightGlass ? 0xFF425860 : 0xFFADC3CA);
        dots.setShadowLayer(.35f * pxPerMm, 0f, 0f, lightGlass ? 0x33403B45 : 0x668BA6B0);
        for (int i = 0; i < 5; i++) {
            float x = start + i * total / 5f;
            float y = dotTop + r;
            float pulse = HomeClockPulse.intensity(separatorPhase, i);
            dots.setAlpha(Math.round(255f * pulse));
            canvas.drawCircle(x, y, r, dots);
        }
        dots.setAlpha(255);
        // Only the minute retains its one-CSS-pixel upward offset; horizontal offset is now zero.
        canvas.save();
        canvas.translate(0f, -cssPx / HOME_SCALE);
        drawDigits(canvas, minute, minuteTop, line);
        canvas.restore();
    }
}
