package com.gt30pro.launcher;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Read-only home label. Android may omit or not support individual properties. */
final class BatteryInfoView extends LinearLayout {
    private final TextView[] names=new TextView[15], values=new TextView[15];
    private final LinearLayout[] cells=new LinearLayout[15];
    private static final String[] LABELS={"LEVEL","STATUS","SUMBER","KONDISI","SUHU","TEGANGAN",
            "ARUS SAAT INI","ARUS RATA-RATA","MUATAN TERSISA","ENERGI TERSISA","SIKLUS","TEKNOLOGI","BATERAI TERDETEKSI","BATERAI RENDAH","PERKIRAAN PENUH"};
    BatteryInfoView(Context c) {
        super(c); setOrientation(VERTICAL); setPadding(dp(12),dp(8),dp(12),dp(8));
        setClickable(false); setFocusable(false); setBackground(null);
        Typeface doto=Typeface.createFromAsset(c.getAssets(),"fonts/Doto.ttf");
        // Keep the old heading's height so all data rows stay in their existing positions.
        TextView spacer=new TextView(c); spacer.setText(" "); spacer.setTextSize(11); spacer.setTypeface(doto); spacer.setFontVariationSettings("'wght' 800");
        spacer.setLetterSpacing(.08f); spacer.setVisibility(View.INVISIBLE); addView(spacer);
        for(int row=0;row<8;row++) {
            LinearLayout line=new LinearLayout(c); line.setOrientation(HORIZONTAL);
            for(int col=0;col<2;col++) {
                int index=row*2+col; if(index>=LABELS.length) break;
                LinearLayout cell=new LinearLayout(c); cell.setOrientation(VERTICAL); cells[index]=cell;
                cell.setPadding(0,dp(3),col==0?dp(6):0,0);
                TextView name=new TextView(c); name.setText(LABELS[index]); name.setTextSize(9); name.setTypeface(doto); name.setFontVariationSettings("'wght' 900");
                name.setSingleLine(true); name.setEllipsize(TextUtils.TruncateAt.END);
                TextView value=new TextView(c); value.setText(BatteryInfoFormat.UNKNOWN); value.setTextSize(12); value.setTypeface(doto); value.setFontVariationSettings("'wght' 900"); value.setSingleLine(true); value.setEllipsize(TextUtils.TruncateAt.END);
                names[index]=name; values[index]=value; cell.addView(name); cell.addView(value); line.addView(cell,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
            }
            addView(line,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        }
        setLightGlass(false);
    }
    void setColumnOffset(float cssPixel) {
        for(int i=1;i<cells.length;i+=2) cells[i].setTranslationX(-30f*cssPixel);
    }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    void setLightGlass(boolean light) {
        for(int i=0;i<names.length;i++) { names[i].setTextColor(0xFFFFCD6A); values[i].setTextColor(light?0xFF17242B:0xFFF1F2E9); }
    }
    private static int property(BatteryManager b,int id) {
        if(b==null) return Integer.MIN_VALUE;
        try { return b.getIntProperty(id); } catch(RuntimeException e) { return Integer.MIN_VALUE; }
    }
    private static long longProperty(BatteryManager b,int id) {
        if(b==null) return Long.MIN_VALUE;
        try { return b.getLongProperty(id); } catch(RuntimeException e) { return Long.MIN_VALUE; }
    }
    private static long timeToFull(BatteryManager b) {
        if(b==null || Build.VERSION.SDK_INT<28) return -1;
        try { return b.computeChargeTimeRemaining(); } catch(RuntimeException e) { return -1; }
    }
    void update(Intent battery) {
        if(battery==null) return;
        BatteryManager b=(BatteryManager)getContext().getSystemService(Context.BATTERY_SERVICE);
        int present=battery.hasExtra(BatteryManager.EXTRA_PRESENT) ? (battery.getBooleanExtra(BatteryManager.EXTRA_PRESENT,false)?1:0) : -1;
        int low=Build.VERSION.SDK_INT>=28 && battery.hasExtra(BatteryManager.EXTRA_BATTERY_LOW) ? (battery.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW,false)?1:0) : -1;
        int cycles=Build.VERSION.SDK_INT>=34 ? battery.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT,-1) : -1;
        String[] data={BatteryInfoFormat.level(battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1)),
            BatteryInfoFormat.status(battery.getIntExtra(BatteryManager.EXTRA_STATUS,-1)),
            BatteryInfoFormat.source(battery.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1)),
            BatteryInfoFormat.health(battery.getIntExtra(BatteryManager.EXTRA_HEALTH,-1)),
            BatteryInfoFormat.temperature(battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE)),
            BatteryInfoFormat.voltage(battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE,-1)),
            BatteryInfoFormat.current(property(b,BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)),
            BatteryInfoFormat.current(property(b,BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)),
            BatteryInfoFormat.charge(property(b,BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)),
            BatteryInfoFormat.energy(longProperty(b,BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)),
            BatteryInfoFormat.cycles(cycles), BatteryInfoFormat.technology(battery.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)),
            BatteryInfoFormat.present(present), BatteryInfoFormat.low(low), BatteryInfoFormat.timeToFull(timeToFull(b))};
        for(int i=0;i<data.length;i++) values[i].setText(data[i]);
    }
}
