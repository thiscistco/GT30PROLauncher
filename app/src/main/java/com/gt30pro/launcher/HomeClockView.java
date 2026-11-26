package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Explicitly sized, software-rendered home clock; charging affects only the dots. */
final class HomeClockView extends View {
    static final float WIDTH_MM = 3.6f;
    static final float CONTENT_HEIGHT_MM = 5.731f;
    static final float GLOW_PAD_MM = 0.4f;
    static final float HEIGHT_MM = CONTENT_HEIGHT_MM + 2f * GLOW_PAD_MM;
    private static final int[] AMBER = {
        0xFFFF6919, 0xFFFF9D2F, 0xFFFFCD6A, 0xFFFF9D2F, 0xFFFF6919
    };
    private static final float[] STOPS = {0f, .24f, .5f, .76f, 1f};
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Paint dots = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String hour = "00", minute = "00";
    private float pxPerMm = 1f, separatorAlpha = 1f;

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

    void setTime(String hours, String minutes) {
        if (!hour.equals(hours) || !minute.equals(minutes)) {
            hour = hours;
            minute = minutes;
            invalidate();
        }
    }

    void setSeparatorAlpha(float alpha) {
        separatorAlpha = Math.max(0f, Math.min(1f, alpha));
        invalidate();
    }

    private void drawDigits(Canvas canvas, String value, float top, float lineHeight) {
        Paint.FontMetrics metrics = text.getFontMetrics();
        float baseline = top + (lineHeight - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent;
        float gap = .2f * pxPerMm;
        float firstWidth = text.measureText(value, 0, 1);
        float secondWidth = text.measureText(value, 1, 2);
        float x = (getWidth() - firstWidth - gap - secondWidth) / 2f;
        text.setShader(new LinearGradient(0, top, 0, top + lineHeight, AMBER, STOPS, Shader.TileMode.CLAMP));
        canvas.drawText(value, 0, 1, x, baseline, text);
        canvas.drawText(value, 1, 2, x + firstWidth + gap, baseline, text);
        text.setShader(null);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float line = 2.3f * pxPerMm;
        float top = GLOW_PAD_MM * pxPerMm;
        drawDigits(canvas, hour, top, line);
        // CSS: 0.461 mm - 1 px above, 0.45 mm + 0.5 px below.
        float cssPx = pxPerMm * 25.4f / 96f;
        float dotTop = top + line + .461f * pxPerMm - cssPx;
        float r = .11f * pxPerMm;
        float total = 2.8f * pxPerMm - .76f * cssPx;
        float center = getWidth() / 2f - .63f * cssPx;
        float start = center - total / 2f + total / 10f;
        dots.setAlpha(Math.round(separatorAlpha * 255));
        dots.setShadowLayer(.6f * pxPerMm, 0f, 0f, 0xA6FF6919);
        for (int i = 0; i < 5; i++) {
            float x = start + i * total / 5f;
            float y = dotTop + r;
            dots.setShader(new RadialGradient(x, y, r,
                new int[]{0xFFFFCD6A, 0xFFFF9D2F, 0xFFFF6919},
                new float[]{0f, .55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(x, y, r, dots);
        }
        dots.setShader(null);
        drawDigits(canvas, minute, dotTop + .22f * pxPerMm + .45f * pxPerMm + .5f * cssPx, line);
    }
}
