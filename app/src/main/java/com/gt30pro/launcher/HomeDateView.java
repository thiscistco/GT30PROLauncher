package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;
import java.time.LocalDate;

/** Live, three-line date using the same Doto amber treatment as the home clock. */
final class HomeDateView extends View {
    // Numeric date: 10 CSS px larger than the previous 2.64 mm text.
    private static final float SIZE_SCALE = (2.64f + 10f * 25.4f / 96f) / 2f;
    // Doto's fixed advance and spacing make 3 letters at 65% as wide as 2 digits.
    private static final float LABEL_SCALE = .65f;
    static final float WIDTH_MM = 5.2f * SIZE_SCALE;
    static final float GLOW_PAD_MM = .4f;
    static final float HEIGHT_MM = 6.9f * SIZE_SCALE + 2f * GLOW_PAD_MM;
    private static final int[] AMBER = {0xFFFF6919, 0xFFFF9D2F, 0xFFFFCD6A, 0xFFFF9D2F, 0xFFFF6919};
    private static final float[] STOPS = {0f, .24f, .5f, .76f, 1f};
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private float pxPerMm = 1f;
    private LocalDate date;

    HomeDateView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setWillNotDraw(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        text.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Doto.ttf"));
        text.setFontVariationSettings("'wght' 800");
        text.setColor(0xFFFFCD6A);
    }
    void setScale(float pixelsPerMm) {
        pxPerMm = pixelsPerMm;
        text.setTextSize(2f * SIZE_SCALE * pxPerMm);
        text.setShadowLayer(.4f * pxPerMm, 0f, 0f, 0xA6FF6919);
        invalidate();
    }
    void setDate(LocalDate value) {
        if (!value.equals(date)) {
            date = value;
            setContentDescription(HomeDateText.description(value));
            invalidate();
        }
    }
    private void drawLine(Canvas canvas, String value, float top, float sizeFactor) {
        float line = 2.3f * SIZE_SCALE * pxPerMm, gap = .2f * SIZE_SCALE * sizeFactor * pxPerMm;
        text.setTextSize(2f * SIZE_SCALE * sizeFactor * pxPerMm);
        Paint.FontMetrics metrics = text.getFontMetrics();
        float baseline = top + (line - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent;
        float width = text.measureText(value) + Math.max(0, value.length() - 1) * gap;
        float x = (getWidth() - width) / 2f;
        text.setShader(new LinearGradient(0, top, 0, top + line, AMBER, STOPS, Shader.TileMode.CLAMP));
        for (int i = 0; i < value.length(); i++) {
            canvas.drawText(value, i, i + 1, x, baseline, text);
            x += text.measureText(value, i, i + 1) + gap;
        }
        text.setShader(null);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (date == null) return;
        float top = GLOW_PAD_MM * pxPerMm;
        drawLine(canvas, HomeDateText.weekday(date), top, LABEL_SCALE);
        drawLine(canvas, HomeDateText.day(date), top + 2.3f * SIZE_SCALE * pxPerMm, 1f);
        drawLine(canvas, HomeDateText.month(date), top + 4.6f * SIZE_SCALE * pxPerMm, LABEL_SCALE);
    }
}
