package com.gt30pro.launcher;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Draws the same dot-matrix waveform as the static icon; only RMS changes its bars. */
final class VoiceWaveDrawable extends Drawable {
    private static final float[] X={1.5f,4f,6.5f,9f,11.5f};
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int size;
    private int[] counts=VoiceWaveLogic.heights(0,0,false);
    VoiceWaveDrawable(Context context) {
        size=Math.round(24*context.getResources().getDisplayMetrics().density);
        paint.setColor(Color.WHITE);
    }
    void setLevel(float rms,int phase) {
        counts=VoiceWaveLogic.heights(rms,phase,ValueAnimator.areAnimatorsEnabled()); invalidateSelf();
    }
    void reset() { counts=VoiceWaveLogic.heights(0,0,false); invalidateSelf(); }
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds(); if(b.isEmpty())return;
        int saved=canvas.save(); canvas.translate(b.left,b.top); canvas.scale(b.width()/13f,b.height()/13f);
        for(int i=0;i<X.length;i++) {
            int start=(13-counts[i])/2;
            for(int y=start;y<start+counts[i];y++) canvas.drawCircle(X[i],y+.5f,.36f,paint);
        }
        canvas.restoreToCount(saved);
    }
    @Override public int getIntrinsicWidth() { return size; }
    @Override public int getIntrinsicHeight() { return size; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
