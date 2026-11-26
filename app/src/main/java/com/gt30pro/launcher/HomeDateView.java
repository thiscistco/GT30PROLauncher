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
    static final float WIDTH_MM = 5.2f;
    static final float GLOW_PAD_MM = .4f;
    static final float HEIGHT_MM = 6.9f + 2f * GLOW_PAD_MM;
    private static final int[] AMBER = {0xFFCB633E, 0xFFFFB568, 0xFFFFE5B1, 0xFFFFB568, 0xFFCB633E};
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
        text.setColor(0xFFFFC477);
    }
    void setScale(float pixelsPerMm) {
        pxPerMm = pixelsPerMm;
        text.setTextSize(2f * pxPerMm);
        text.setShadowLayer(.4f * pxPerMm, 0f, 0f, 0xA6DD6234);
        invalidate();
    }
    void setDate(LocalDate value) {
        if (!value.equals(date)) {
            date = value;
            setContentDescription(HomeDateText.description(value));
            invalidate();
        }
    }
    private void drawLine(Canvas canvas, String value, float top) {
        float line = 2.3f * pxPerMm, gap = .2f * pxPerMm;
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
        drawLine(canvas, HomeDateText.weekday(date), top);
        drawLine(canvas, HomeDateText.day(date), top + 2.3f * pxPerMm);
        drawLine(canvas, HomeDateText.month(date), top + 4.6f * pxPerMm);
    }
}
