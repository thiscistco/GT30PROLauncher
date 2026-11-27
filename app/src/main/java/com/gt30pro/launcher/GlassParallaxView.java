package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

/** Subtle moving specular reflection; the glass image and controls never move. */
final class GlassParallaxView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix=new Matrix();
    private LinearGradient sheen;
    private float x,y;
    GlassParallaxView(Context c) {
        super(c); setClickable(false); setFocusable(false); setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setVisibility(View.INVISIBLE);
    }
    void setTilt(float x,float y) { this.x=x; this.y=y; invalidate(); }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        if(w<=0 || h<=0) { sheen=null; return; }
        sheen=new LinearGradient(-.2f*w,.15f*h,1.2f*w,.55f*h,
                new int[]{0x00FFFFFF,0x00FFFFFF,0x10FFFFFF,0x24FFFFFF,0x06FFFFFF,0x00FFFFFF},
                new float[]{0f,.34f,.44f,.49f,.54f,.65f},Shader.TileMode.CLAMP);
        paint.setShader(sheen);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); if(sheen==null) return;
        matrix.setTranslate(x*getWidth()*.035f,y*getHeight()*.035f);
        sheen.setLocalMatrix(matrix);
        canvas.drawRect(0,0,getWidth(),getHeight(),paint);
    }
}
