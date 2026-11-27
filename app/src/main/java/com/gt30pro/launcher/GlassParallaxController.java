package com.gt30pro.launcher;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.WindowManager;

/** Sensors run only while the home activity is active and effects are allowed. */
final class GlassParallaxController implements SensorEventListener {
    private final Context context;
    private final GlassParallaxView view;
    private final GlassParallaxMotion motion=new GlassParallaxMotion();
    private SensorManager manager;
    private boolean registered;
    GlassParallaxController(Context c,GlassParallaxView view) { context=c; this.view=view; }
    void start() {
        if(registered) return;
        try {
            manager=(SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
            if(manager==null) return;
            Sensor sensor=manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            if(sensor==null) sensor=manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if(sensor==null) return;
            motion.reset();
            registered=manager.registerListener(this,sensor,50000);
        } catch(RuntimeException ignored) { registered=false; }
    }
    void stop() {
        if(manager!=null && registered) try { manager.unregisterListener(this); } catch(RuntimeException ignored) { }
        registered=false; motion.reset(); view.setVisibility(View.INVISIBLE);
    }
    void recenter() { motion.reset(); }
    @Override public void onSensorChanged(SensorEvent event) {
        if(!registered || event==null || event.values==null || event.values.length<2) return;
        if(!android.animation.ValueAnimator.areAnimatorsEnabled()) { stop(); return; }
        int rotation;
        try {
            WindowManager wm=(WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            if(wm==null) return;
            rotation=wm.getDefaultDisplay().getRotation();
        } catch(RuntimeException ignored) { return; }
        if(motion.update(event.values[0],event.values[1],rotation)) {
            view.setTilt(motion.x(),motion.y());
            if(view.getVisibility()!=View.VISIBLE) view.setVisibility(View.VISIBLE);
        }
    }
    @Override public void onAccuracyChanged(Sensor sensor,int accuracy) { }
}
