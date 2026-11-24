package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * Tampilan angka gaya LED dot-matrix (grid titik 5x7 + glow)
 * untuk indikator baterai di area USB-C.
 */
public class DotMatrixView extends View {

    private static final int COLS = 5;
    private static final int ROWS = 7;

    /** Pola titik 5x7 untuk glyph '0'-'9' dan '%'. */
    private static final String[][] GLYPHS = {
            {"01110", "10001", "10011", "10101", "11001", "10001", "01110"}, // 0
            {"00100", "01100", "00100", "00100", "00100", "00100", "01110"}, // 1
            {"01110", "10001", "00001", "00010", "00100", "01000", "11111"}, // 2
            {"11111", "00010", "00100", "00010", "00001", "10001", "01110"}, // 3
            {"00010", "00110", "01010", "10010", "11111", "00010", "00010"}, // 4
            {"11111", "10000", "11110", "00001", "00001", "10001", "01110"}, // 5
            {"00110", "01000", "10000", "11110", "10001", "10001", "01110"}, // 6
            {"11111", "00001", "00010", "00100", "01000", "01000", "01000"}, // 7
            {"01110", "10001", "10001", "01110", "10001", "10001", "01110"}, // 8
            {"01110", "10001", "10001", "01111", "00001", "00010", "01100"}, // 9
            {"11000", "11001", "00010", "00100", "01000", "10011", "00011"}, // %
    };

    private String text = "85%";
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float step;

    public DotMatrixView(Context context) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        step = 5f * density;
        dotPaint.setColor(0xFFFFB44A);
        glowPaint.setColor(0x55FFB44A);
    }

    public void setText(String text) {
        this.text = (text == null) ? "" : text;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float r = step * 0.36f;
        int w = (int) ((text.length() * COLS - 1) * step + 2 * r) + getPaddingLeft() + getPaddingRight();
        int h = (int) ((ROWS - 1) * step + 2 * r) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = step * 0.36f;
        float x0 = getPaddingLeft() + r;
        float y0 = getPaddingTop() + r;
        for (int i = 0; i < text.length(); i++) {
            String[] glyph = glyphFor(text.charAt(i));
            for (int row = 0; row < ROWS; row++) {
                String line = glyph[row];
                for (int col = 0; col < COLS; col++) {
                    if (line.charAt(col) == '1') {
                        float cx = x0 + (i * COLS + col) * step;
                        float cy = y0 + row * step;
                        canvas.drawCircle(cx, cy, r * 2.6f, glowPaint);
                        canvas.drawCircle(cx, cy, r, dotPaint);
                    }
                }
            }
        }
    }

    private String[] glyphFor(char c) {
        if (c >= '0' && c <= '9') {
            return GLYPHS[c - '0'];
        }
        return GLYPHS[10]; // '%'
    }
}
