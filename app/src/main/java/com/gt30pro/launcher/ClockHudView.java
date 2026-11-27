package com.gt30pro.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import java.util.Calendar;
import java.util.Locale;

/** Local digital time in a circular dial; the marker advances once per second. */
final class ClockHudView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    private final Typeface doto;
    private final int ink, muted, border, card;
    private String value = "00:00:00";
    private int second;

    ClockHudView(Context context, AppUi ui, Typeface font) {
        super(context);
        doto=font; ink=ui.ink; muted=ui.muted; border=ui.border; card=ui.card;
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        setTime(Calendar.getInstance());
    }
    void setTime(Calendar time) {
        int h=time.get(Calendar.HOUR_OF_DAY), m=time.get(Calendar.MINUTE), s=time.get(Calendar.SECOND);
        value=String.format(Locale.US,"%02d:%02d:%02d",h,m,s);
        second=s;
        setContentDescription("Jam " + value);
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density=getResources().getDisplayMetrics().density;
        float gap=Math.min(8f*density, Math.min(getWidth(),getHeight())*.05f);
        RectF frame=new RectF(gap,gap,getWidth()-gap,getHeight()-gap);
        if (frame.width() <= 0 || frame.height() <= 0) return;
        float corner=Math.min(12f*density,Math.min(frame.width(),frame.height())/2f);
        paint.setStyle(Paint.Style.FILL); paint.setColor(card); canvas.drawRoundRect(frame,corner,corner,paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.max(1f,density)); paint.setColor(border);
        canvas.drawRoundRect(frame,corner,corner,paint);
        float size=Math.min(frame.width(),frame.height());
        float cx=getWidth()/2f, cy=getHeight()/2f, radius=size*.43f;
        paint.setStyle(Paint.Style.FILL); paint.setColor(card); canvas.drawCircle(cx,cy,radius,paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.max(1f,size*.004f)); paint.setColor(border);
        canvas.drawCircle(cx,cy,radius,paint);
        paint.setStrokeWidth(Math.max(1f,size*.002f)); canvas.drawCircle(cx,cy,radius*.78f,paint);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i=0; i<60; i++) {
            double angle=Math.PI * 2 * i/60 - Math.PI/2;
            float inner=radius*(i%5==0 ? .84f : .89f), outer=radius*.94f;
            paint.setColor(i%5==0 ? ink : muted); paint.setStrokeWidth(Math.max(1f,size*(i%5==0 ? .007f : .003f)));
            canvas.drawLine(cx+(float)Math.cos(angle)*inner,cy+(float)Math.sin(angle)*inner,
                    cx+(float)Math.cos(angle)*outer,cy+(float)Math.sin(angle)*outer,paint);
        }
        double angle=Math.PI*2*second/60-Math.PI/2;
        paint.setStyle(Paint.Style.FILL); paint.setColor(ink);
        canvas.drawCircle(cx+(float)Math.cos(angle)*radius*.965f,cy+(float)Math.sin(angle)*radius*.965f,Math.max(2f,size*.012f),paint);
        paint.setTypeface(doto); paint.setFontVariationSettings("'wght' 800"); paint.setTextSize(size*.135f); paint.setTextAlign(Paint.Align.CENTER);
        float maxWidth=radius*1.52f, width=paint.measureText(value);
        if (width>maxWidth) paint.setTextSize(paint.getTextSize()*maxWidth/width);
        Paint.FontMetrics metrics=paint.getFontMetrics();
        canvas.drawText(value,cx,cy-(metrics.ascent+metrics.descent)/2f,paint);
    }
}
