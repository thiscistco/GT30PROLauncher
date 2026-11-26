package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import java.io.InputStream;

/** A shared static sky image fading to black behind the weather page only. */
final class WeatherSkyDrawable extends Drawable {
    private final Bitmap sky;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    WeatherSkyDrawable(Context context) {
        Bitmap image = null;
        try (InputStream stream = context.getAssets().open("weather_sky.jpg")) { image = BitmapFactory.decodeStream(stream); }
        catch (Exception ignored) { }
        sky = image;
    }
    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        paint.setShader(null); paint.setColor(Color.BLACK); paint.setAlpha(255);
        canvas.drawRect(bounds, paint);
        if (sky == null || bounds.width() <= 0 || bounds.height() <= 0) return;
        float height = bounds.width() * 80f / 70.9f;
        int sourceHeight = Math.max(1, Math.min(sky.getHeight(), Math.round(sky.getWidth() * height / bounds.width())));
        paint.setShader(null); paint.setColor(Color.WHITE); paint.setAlpha(255);
        canvas.drawBitmap(sky, new Rect(0, 0, sky.getWidth(), sourceHeight),
                new RectF(bounds.left, bounds.top, bounds.right, bounds.top + height), paint);
        paint.setShader(new LinearGradient(0, bounds.top, 0, bounds.top + height,
                new int[]{0x4D000000,0xA6000000,0xFF000000}, new float[]{0f,.575f,1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(bounds.left,bounds.top,bounds.right,bounds.bottom,paint);
        paint.setShader(null);
    }
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
