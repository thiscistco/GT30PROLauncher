package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Doto dot-grid weather: temperature on home, icon only in Super App. */
final class WeatherView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private int code = -1;
    private boolean day = true;
    private String temperature = "--°";
    private int color;
    private final boolean home;
    private boolean opensDetails;
    private float homeCssPixel;
    WeatherView(Context context, boolean home) {
        super(context);
        this.home = home;
        color = home ? 0xFFFF9D2F : Color.WHITE;
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        paint.setTypeface(Typeface.createFromAsset(context.getAssets(), "fonts/Doto.ttf"));
        paint.setFontVariationSettings("'wght' 700");
        setClickable(true);
        setFocusable(true);
        setWeather(-1, true, null, "Ketuk untuk mengaktifkan cuaca");
    }
    void setLightGlass(boolean light) {
        if (home) { color = light ? 0xFF993800 : 0xFFFF9D2F; invalidate(); }
    }
    void setOpensDetails(boolean value) { opensDetails = value; }
    // Use the root's physical CSS-pixel scale so the 5px gap adjustment matches preview.
    void setHomeCssPixel(float value) {
        if (!home || homeCssPixel == value) return;
        homeCssPixel = value;
        invalidate();
    }
    void setWeather(int weatherCode, boolean isDay, Integer temp, String status) {
        code = weatherCode; day = isDay;
        temperature = temp == null ? "--°" : temp + "°";
        setContentDescription("Cuaca: " + WeatherIcons.description(code, day) + ", "
                + (temp == null ? "suhu belum tersedia" : temp + " derajat Celsius") + ". "
                + (opensDetails ? "Ketuk untuk membuka Cuaca" : status));
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float s = home ? Math.min(getWidth() / 20f, getHeight() / 25f)
                : Math.min(getWidth(), getHeight()) * (50f / 76f) / 13f;
        float x0 = (getWidth() - 13f*s) / 2f;
        float y0 = (getHeight() - (home ? 22f : 13f)*s) / 2f;
        paint.setColor(color); paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(.65f*s, 0, 0, color == Color.WHITE ? 0 : (color == 0xFF993800 ? 0x44893000 : 0x99FF6919));
        String[] rows = WeatherIcons.forCode(code, day);
        for (int y = 0; y < 13; y++) for (int x = 0; x < 13; x++) {
            if (rows[y].charAt(x) == '#') canvas.drawCircle(x0+(x+.5f)*s, y0+(y+.5f)*s, .36f*s, paint);
        }
        if (home) {
            paint.setTextSize(5.5f*s);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(temperature, getWidth()/2f, y0+20.8f*s - 5f*homeCssPixel, paint);
        }
        paint.clearShadowLayer();
    }
}
