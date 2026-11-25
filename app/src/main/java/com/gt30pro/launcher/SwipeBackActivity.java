package com.gt30pro.launcher;

import android.app.Activity;
import android.view.MotionEvent;

/** Swipe dari tepi kanan ke kiri untuk kembali pada halaman internal launcher. */
public abstract class SwipeBackActivity extends Activity {
    private static final float EDGE_DP = 32f;
    private static final float DISTANCE_DP = 64f;
    private float downX;
    private float downY;
    private boolean candidate;
    private boolean intercepting;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float density = getResources().getDisplayMetrics().density;
        if (action == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            intercepting = false;
            candidate = getWindow().getDecorView().getWidth() - downX <= EDGE_DP * density;
            boolean handled = super.dispatchTouchEvent(event);
            return candidate || handled;
        }
        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            candidate = false;
        }
        if (action == MotionEvent.ACTION_MOVE && candidate && !intercepting) {
            float dx = downX - event.getX();
            float dy = Math.abs(event.getY() - downY);
            if (dy > DISTANCE_DP * density && dy > Math.abs(dx)) {
                candidate = false;
            } else if (dx >= DISTANCE_DP * density && dx > dy * 1.5f) {
                // Batalkan klik/scroll child sebelum mengambil alih sisa gesture.
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
                intercepting = true;
            }
        }
        if (intercepting) {
            if (action == MotionEvent.ACTION_UP) {
                float dx = downX - event.getX();
                float dy = Math.abs(event.getY() - downY);
                boolean complete = candidate && dx >= DISTANCE_DP * density && dx > dy * 1.5f;
                intercepting = false;
                candidate = false;
                if (complete) onBackPressed();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                intercepting = false;
                candidate = false;
            }
            return true;
        }
        boolean handled = super.dispatchTouchEvent(event);
        boolean wasCandidate = candidate;
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) candidate = false;
        return handled || wasCandidate;
    }
}
