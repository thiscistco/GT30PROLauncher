package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONObject;

/** Cahaya bergerak di sepanjang jalur, dibatasi mask LED (baterai/kabel terlindung). */
public class CircuitGlowView extends View {
    private final Paint light = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap mask;
    private PathSpec[] paths = new PathSpec[0];
    private int sourceWidth = 1224, sourceHeight = 2720;
    private long durationMs = 5500;
    private float radius = 80, phase;
    private final RectF sourceRect = new RectF();
    private final float[] position = new float[2];
    private final PorterDuffXfermode clipMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);

    private static class PathSpec {
        float offset, length;
        float[] xs, ys, segments;
        final android.graphics.Matrix matrix = new android.graphics.Matrix();
        RadialGradient gradient;
        void position(float fraction, float[] result) {
            float distance = fraction * length;
            for (int i = 0; i < segments.length; i++) {
                if (distance <= segments[i] || i == segments.length - 1) {
                    float t = segments[i] == 0 ? 0 : Math.min(1, distance / segments[i]);
                    result[0] = xs[i] + (xs[i + 1] - xs[i]) * t;
                    result[1] = ys[i] + (ys[i + 1] - ys[i]) * t;
                    return;
                }
                distance -= segments[i];
            }
        }
    }

    public CircuitGlowView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void load() {
        mask = Wallpapers.loadAmberGlow(getContext());
        try (InputStream input = getContext().getAssets().open("circuit_paths.json");
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            JSONObject data = new JSONObject(bytes.toString("UTF-8"));
            sourceWidth = data.getInt("width"); sourceHeight = data.getInt("height");
            radius = (float) data.getDouble("radius"); durationMs = data.getLong("durationMs");
            JSONArray items = data.getJSONArray("paths"); paths = new PathSpec[items.length()];
            for (int i = 0; i < items.length(); i++) {
                JSONObject obj = items.getJSONObject(i); JSONArray points = obj.getJSONArray("points");
                PathSpec path = new PathSpec(); path.offset = (float)obj.getDouble("offset");
                path.gradient = new RadialGradient(0, 0, radius,
                        new int[]{0xFFFFFFFF, 0xA6FFFFFF, Color.TRANSPARENT},
                        new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
                path.xs = new float[points.length()]; path.ys = new float[points.length()];
                path.segments = new float[Math.max(0, points.length() - 1)];
                for (int j = 0; j < points.length(); j++) {
                    JSONArray point = points.getJSONArray(j);
                    path.xs[j] = (float)point.getDouble(0); path.ys[j] = (float)point.getDouble(1);
                    if (j > 0) {
                        path.segments[j - 1] = (float)Math.hypot(path.xs[j] - path.xs[j - 1], path.ys[j] - path.ys[j - 1]);
                        path.length += path.segments[j - 1];
                    }
                }
                paths[i] = path;
            }
            sourceRect.set(0, 0, sourceWidth, sourceHeight);
        } catch (Exception ignored) { paths = new PathSpec[0]; }
    }
    public boolean isReady() { return mask != null && paths.length > 0; }
    public long getDurationMs() { return durationMs; }
    public void setPhase(float value) { phase = value; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isReady() || getWidth() == 0 || getHeight() == 0) return;
        float scale = Math.max((float)getWidth() / sourceWidth, (float)getHeight() / sourceHeight);
        float dx = (getWidth() - sourceWidth * scale) / 2f;
        float dy = (getHeight() - sourceHeight * scale) / 2f;
        int before = canvas.save(); canvas.translate(dx, dy); canvas.scale(scale, scale);
        int layer = canvas.saveLayer(sourceRect, null);
        for (PathSpec path : paths) {
            float fraction = phase + path.offset; fraction -= (float)Math.floor(fraction);
            path.position(fraction, position);
            path.matrix.setTranslate(position[0], position[1]);
            path.gradient.setLocalMatrix(path.matrix);
            light.setShader(path.gradient);
            canvas.drawCircle(position[0], position[1], radius, light);
        }
        light.setShader(null);
        maskPaint.setXfermode(clipMode);
        canvas.drawBitmap(mask, null, sourceRect, maskPaint);
        maskPaint.setXfermode(null);
        canvas.restoreToCount(layer); canvas.restoreToCount(before);
    }
}
