package com.gt30pro.launcher;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.view.DragEvent;
import android.content.ClipData;
import android.os.SystemClock;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

/** Pusat aplikasi bawaan dalam grid. */
public class SuperAppActivity extends SwipeBackActivity {
    private WeatherController weather;
    private AppUi ui;
    private WeatherView weatherView;
    private long firstHomeSwipe;
    private boolean secondCandidate, secondIntercepting;
    private float secondX, secondY;
    private boolean doubleSwipeWindow() {
        long age=SystemClock.elapsedRealtime()-firstHomeSwipe;
        return firstHomeSwipe>0 && age>=0 && age<=2000;
    }
    @Override protected boolean supportsBottomSwipe() { return !doubleSwipeWindow(); }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action=event.getActionMasked();
        if (action==MotionEvent.ACTION_DOWN) {
            secondCandidate=doubleSwipeWindow(); secondIntercepting=false;
            secondX=event.getX(); secondY=event.getY();
            boolean handled=super.dispatchTouchEvent(event);
            return secondCandidate || handled;
        }
        if(action==MotionEvent.ACTION_POINTER_DOWN) secondCandidate=false;
        float density=getResources().getDisplayMetrics().density;
        if(action==MotionEvent.ACTION_MOVE && secondCandidate && !secondIntercepting &&
                HomeGesture.vertical(event.getX()-secondX,event.getY()-secondY,12f*density) && event.getY()<secondY) {
            MotionEvent cancel=MotionEvent.obtain(event); cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.dispatchTouchEvent(cancel); cancel.recycle(); secondIntercepting=true;
        }
        if(secondIntercepting) {
            if(action==MotionEvent.ACTION_UP) {
                int gesture=HomeGesture.action(secondX,secondY,event.getX(),event.getY(),getWindow().getDecorView().getWidth(),getWindow().getDecorView().getHeight(),density);
                boolean open=secondCandidate && doubleSwipeWindow() && gesture==HomeGesture.SUPER;
                secondCandidate=false; secondIntercepting=false;
                if(open) { firstHomeSwipe=0; startActivity(new Intent(this,AppDrawerActivity.class)); }
            } else if(action==MotionEvent.ACTION_CANCEL) { secondCandidate=false; secondIntercepting=false; }
            return true;
        }
        boolean handled=super.dispatchTouchEvent(event);
        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) secondCandidate=false;
        return handled;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firstHomeSwipe=getIntent().getLongExtra("home_swipe_time",0);
        setContentView(R.layout.activity_super_app);
        ui = new AppUi(this); ui.window(this);

        View root = findViewById(R.id.root);
        root.setBackgroundColor(ui.canvas);
        android.widget.LinearLayout content = findViewById(R.id.content);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            content.setPadding(dp(20), dp(16) + insets.getSystemWindowInsetTop(),
                    dp(20), dp(24) + insets.getSystemWindowInsetBottom());
            return insets;
        });

        Typeface doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        content.removeView(findViewById(R.id.back)); content.removeView(findViewById(R.id.super_title));
        content.addView(ui.header(this, doto, "Super App"),0);

        GridLayout container = findViewById(R.id.modules_container);
        addModule(container, R.drawable.ic_module_clock, "Jam", ClockActivity.class);
        addModule(container, R.drawable.ic_module_calendar, "Kalender", CalendarActivity.class);
        weatherView = new WeatherView(this, false);
        weatherView.setIconColor(ui.ink);
        weatherView.setBackground(ui.iconGlass(this,false));
        GridLayout.LayoutParams weatherParams = new GridLayout.LayoutParams();
        weatherParams.width = 1; weatherParams.height = dp(76);
        container.addView(weatherView, weatherParams);
        weather = new WeatherController(this, weatherView);
        weatherView.setOpensDetails(true);
        weatherView.setContentDescription("Buka Cuaca");
        weatherView.setOnClickListener(v -> startActivity(new Intent(this, WeatherActivity.class)));
        addModule(container, R.drawable.ic_module_timetable, "Jadwal Mengajar", TimeTableActivity.class);
        addModule(container, R.drawable.ic_module_settings, "Setelan", LauncherSettingsActivity.class);
        setupReordering(container);
        container.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
            if (r - l != oldR - oldL) sizeGridCells(container);
        });
        container.post(() -> sizeGridCells(container));
    }

    @Override protected void onResume() { super.onResume(); AppUi current = new AppUi(this); if (ui != null && (ui.dark != current.dark || !ui.choice.equals(current.choice))) { recreate(); return; } if (weather != null) weather.resume(); }
    @Override protected void onPause() { if (weather != null) weather.pause(); super.onPause(); }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (weather != null) weather.permissionResult(requestCode);
    }

    private void addModule(GridLayout grid, int icon, String name, Class<?> activity) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(icon);
        button.setColorFilter(ui.ink);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER);
        button.setPadding(dp(13), dp(13), dp(13), dp(13));
        button.setBackground(ui.iconGlass(this,false));
        button.setContentDescription(name);
        button.setOnClickListener(v -> startActivity(new Intent(this, activity)));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 1;
        params.height = dp(76);
        grid.addView(button, params);
    }

    private void sizeGridCells(GridLayout grid) {
        int width = grid.getWidth();
        if (width <= 0) return;
        int cellWidth = Math.max(1, width / 4);
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            GridLayout.LayoutParams params = (GridLayout.LayoutParams) child.getLayoutParams();
            if (params.width != cellWidth || params.height != cellWidth) {
                params.width = cellWidth; params.height = cellWidth;
                child.setLayoutParams(params);
            }
            if (child instanceof ImageButton) {
                int pad=Math.max(0,(cellWidth-dp(50))/2);
                child.setPadding(pad,pad,pad,pad);
            }
        }
    }

    private void setupReordering(GridLayout grid) {
        String[] ids={"clock","calendar","weather","timetable","settings"};
        for (int i=0;i<ids.length;i++) grid.getChildAt(i).setTag(ids[i]);
        java.util.ArrayList<String> order=ModuleOrder.parse(getSharedPreferences("super_app",MODE_PRIVATE).getString("order",""));
        for(int target=0;target<order.size();target++) {
            for(int i=target;i<grid.getChildCount();i++) if(order.get(target).equals(grid.getChildAt(i).getTag())) {
                View item=grid.getChildAt(i); grid.removeViewAt(i); grid.addView(item,target); break;
            }
        }
        for(int i=0;i<grid.getChildCount();i++) {
            final View child=grid.getChildAt(i);
            child.setOnLongClickListener(v -> v.startDragAndDrop(ClipData.newPlainText("module", ""),new View.DragShadowBuilder(v),v,0));
            child.setOnDragListener((v,event) -> {
                if (event.getAction()==DragEvent.ACTION_DRAG_STARTED) return event.getLocalState() instanceof View && ((View)event.getLocalState()).getParent()==grid;
                if (event.getAction()==DragEvent.ACTION_DROP) return moveModule(grid,(View)event.getLocalState(),grid.indexOfChild(v));
                return true;
            });
        }
    }
    private boolean moveModule(GridLayout grid,View source,int target) {
        int from=grid.indexOfChild(source);
        if(from<0 || target<0 || target>=grid.getChildCount()) return false;
        if(from!=target) { grid.removeViewAt(from); grid.addView(source,target); }
        StringBuilder value=new StringBuilder();
        for(int i=0;i<grid.getChildCount();i++) { if(i>0)value.append(','); value.append(grid.getChildAt(i).getTag()); }
        getSharedPreferences("super_app",MODE_PRIVATE).edit().putString("order",value.toString()).apply();
        sizeGridCells(grid); return true;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
