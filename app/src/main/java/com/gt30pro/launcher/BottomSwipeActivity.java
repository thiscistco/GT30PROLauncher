package com.gt30pro.launcher;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.KeyEvent;

/** Intercepts only an upward drag starting near the bottom, before child scrolling or clicks. */
public abstract class BottomSwipeActivity extends Activity {
    private float startX, startY, startHeight;
    private boolean candidate, intercepting;

    private final VoiceVolumeShortcut volumeShortcut=new VoiceVolumeShortcut();
    @Override protected void onResume() { super.onResume(); VoiceController.resume(this); VoiceController.attachFloatingButton(this); }
    @Override protected void onPause() { VoiceController.pause(this); super.onPause(); }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request,permissions,results); VoiceController.permissionResult(this,request);
    }
    boolean handleVoiceVolumeKey(KeyEvent event) {
        int key=event.getKeyCode();
        if ((key!=KeyEvent.KEYCODE_VOLUME_UP && key!=KeyEvent.KEYCODE_VOLUME_DOWN) || event.getAction()!=KeyEvent.ACTION_DOWN || event.getRepeatCount()!=0) return false;
        if (volumeShortcut.press(key,event.getEventTime())) {
            VoiceController.toggle(this,key==KeyEvent.KEYCODE_VOLUME_UP ? VoiceCommands.COMMAND : VoiceCommands.TYPING); return true;
        }
        return false;
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) { return handleVoiceVolumeKey(event) || super.dispatchKeyEvent(event); }
    protected boolean handleVoiceAction(String action) { return false; }

    protected boolean supportsBottomSwipe() { return false; }
    protected void onBottomSwipe() { }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            startX = event.getX(); startY = event.getY();
            startHeight = getWindow().getDecorView().getHeight();
            intercepting = false;
            candidate = supportsBottomSwipe() && BottomSwipe.startsAtBottom(startY, startHeight);
            boolean handled = super.dispatchTouchEvent(event);
            return candidate || handled;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) candidate = false;
        if (action == MotionEvent.ACTION_MOVE && candidate && !intercepting &&
                BottomSwipe.shouldIntercept(startX, startY, event.getX(), event.getY(), getResources().getDisplayMetrics().density)) {
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel);
            cancel.recycle();
            intercepting = true;
        }
        if (intercepting) {
            if (action == MotionEvent.ACTION_UP) {
                boolean complete = candidate && BottomSwipe.completes(startX, startY, event.getX(), event.getY(), startHeight);
                candidate = false; intercepting = false;
                if (complete) onBottomSwipe();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                candidate = false; intercepting = false;
            }
            return true;
        }
        boolean handled = super.dispatchTouchEvent(event);
        boolean wasCandidate = candidate;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) candidate = false;
        return handled || wasCandidate;
    }
}
