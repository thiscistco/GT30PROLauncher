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
    // Scale the previous numeric date and its labels down by another ten percent.
    private static final float SIZE_SCALE = .855f * (2.64f + 10f * 25.4f / 96f) / 2f;
    // Doto's fixed advance and spacing make 3 letters at 65% as wide as 2 digits.
    private static final float LABEL_SCALE = .65f;
    static final float WIDTH_MM = 5.2f * SIZE_SCALE;
    static final float GLOW_PAD_MM = .4f;
    // Three line boxes at line-height 1 plus a 1 CSS px gap between them.
    static final float CSS_PX_MM = 25.4f / 96f;
    static final float TEXT_MM = 2f * SIZE_SCALE;
    static final float CONTENT_HEIGHT_MM = TEXT_MM * (1f + 2f * LABEL_SCALE) + 2f * CSS_PX_MM;
    static final float HEIGHT_MM = CONTENT_HEIGHT_MM + 2f * GLOW_PAD_MM;
    private static final int[] AMBER = {0xFFFF9D2F, 0xFFFFAA46, 0xFFFFB65C, 0xFFFFAA46, 0xFFFF9D2F};
    private static final int[] AMBER_LIGHT = {0xFF7A2D00, 0xFF893000, 0xFF993800, 0xFF893000, 0xFF7A2D00};
    private boolean lightGlass;
    private static final float[] STOPS = {0f, .24f, .5f, .76f, 1f};
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private float pxPerMm = -1f;
    private LocalDate date;

    HomeDateView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setWillNotDraw(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        text.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Doto.ttf"));
        text.setFontVariationSettings("'wght' 800");
        text.setColor(0xFFFFB65C);
    }
    void setScale(float pixelsPerMm) {
        if (pxPerMm == pixelsPerMm) return;
        pxPerMm = pixelsPerMm;
        text.setTextSize(2f * SIZE_SCALE * pxPerMm);
        text.setShadowLayer(.4f * pxPerMm, 0f, 0f, 0xA6FF6919);
        invalidate();
    }
    void setLightGlass(boolean light) {
        if (lightGlass != light) { lightGlass = light; invalidate(); }
    }
    void setDate(LocalDate value) {
        if (!value.equals(date)) {
            date = value;
            setContentDescription(HomeDateText.description(value) + ". Buka Kalender di Super App");
            invalidate();
        }
    }
    private void drawLine(Canvas canvas, String value, float top, float sizeFactor, boolean gunmetal) {
        float line = TEXT_MM * sizeFactor * pxPerMm, gap = .2f * SIZE_SCALE * sizeFactor * pxPerMm;
        text.setTextSize(2f * SIZE_SCALE * sizeFactor * pxPerMm);
        text.setColor(lightGlass ? (gunmetal ? 0xFF263840 : 0xFF993800)
                : (gunmetal ? 0xFFAABAC5 : 0xFFFFB65C));
        text.setShadowLayer(.4f * pxPerMm, 0f, 0f, lightGlass ? 0x33283B45 : (gunmetal ? 0xD9141B1F : 0xA6FF6919));
        Paint.FontMetrics metrics = text.getFontMetrics();
        float baseline = top + (line - (metrics.descent - metrics.ascent)) / 2f - metrics.ascent;
        float width = text.measureText(value) + Math.max(0, value.length() - 1) * gap;
        float x = (getWidth() - width) / 2f;
        text.setShader(gunmetal ? null : new LinearGradient(0, top, 0, top + line, lightGlass ? AMBER_LIGHT : AMBER, STOPS, Shader.TileMode.CLAMP));
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
        float labelLine = TEXT_MM * LABEL_SCALE * pxPerMm;
        float numberLine = TEXT_MM * pxPerMm;
        float rowGap = CSS_PX_MM * pxPerMm;
        drawLine(canvas, HomeDateText.weekday(date), top, LABEL_SCALE, true);
        drawLine(canvas, HomeDateText.day(date), top + labelLine + rowGap, 1f, false);
        drawLine(canvas, HomeDateText.month(date), top + labelLine + numberLine + 2f * rowGap, LABEL_SCALE, true);
    }
}
