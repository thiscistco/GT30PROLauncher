package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** A tiny dotted lightning mark in the same dot-matrix spirit as Doto. */
final class BatteryBoltView extends View {
    static final float WIDTH_MM = 2.8125f;
    static final float HEIGHT_MM = 3.9375f;
    // High bit is the leftmost of five columns; matches the preview SVG.
    private static final int[] ROWS = {6, 12, 12, 30, 6, 12, 8};
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);

    BatteryBoltView(Context context) {
        super(context);
        dot.setColor(0xFFFFFFFF);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
    }

    void setColor(int color) {
        dot.setColor(color);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cellWidth = getWidth() / 5f, cellHeight = getHeight() / 7f;
        float radius = Math.min(cellWidth, cellHeight) * .32f;
        for (int row = 0; row < ROWS.length; row++) {
            for (int col = 0; col < 5; col++) {
                if ((ROWS[row] & (1 << (4 - col))) != 0) {
                    canvas.drawCircle((col + .5f) * cellWidth, (row + .5f) * cellHeight, radius, dot);
                }
            }
        }
    }
}
