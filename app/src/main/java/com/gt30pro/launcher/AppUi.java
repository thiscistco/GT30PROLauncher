package com.gt30pro.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/** Shared visual language for launcher-owned applications, based on Calendar. */
final class AppUi {
    final boolean dark;
    final int canvas, card, ink, muted, control, border, selected, selectedText, event;
    final String choice;
    AppUi(Context context) {
        choice = context.getSharedPreferences("calendar_appearance", Context.MODE_PRIVATE).getString("theme", "system");
        dark = "dark".equals(choice) || (!"light".equals(choice) &&
                (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        canvas = dark ? 0xFF101820 : 0xFFF4F7FA;
        card = dark ? 0xFF19252F : Color.WHITE;
        ink = dark ? 0xFFF5F8FA : 0xFF142A3A;
        muted = dark ? 0xFFC7D3DD : 0xFF405465;
        control = dark ? 0xFF2A3B49 : 0xFFE3EAF0;
        border = dark ? 0xFF516776 : 0xFFC9D5DE;
        selected = dark ? 0xFFDCE8F0 : 0xFF304355;
        selectedText = dark ? 0xFF172A38 : Color.WHITE;
        event = dark ? 0xFF263743 : 0xFFE5ECF2;
    }
    int dp(Context c, int n) { return Math.round(n*c.getResources().getDisplayMetrics().density); }
    GradientDrawable surface(Context c, int color) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c,12)); d.setStroke(dp(c,1),border); return d;
    }
    /** Frosted-looking translucent placeholder; no screen capture or platform blur. */
    GradientDrawable iconGlass(Context c, boolean selected) {
        int top=dark ? (selected ? 0xCC7893A4 : 0x88728A9A) : (selected ? 0xDD91AAB8 : 0xAAF8FCFF);
        int bottom=dark ? (selected ? 0xCC344D5D : 0x88314958) : (selected ? 0xDDB2CAD5 : 0xAAB8CFDB);
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});
        d.setCornerRadius(dp(c,12)); d.setStroke(dp(c,1),dark ? 0xAAADC5D2 : 0xAA7B98A7); return d;
    }
    void window(Activity activity) {
        activity.getWindow().setStatusBarColor(canvas); activity.getWindow().setNavigationBarColor(canvas);
        activity.getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }
    AlertDialog.Builder dialog(Activity a) {
        return new AlertDialog.Builder(a, dark ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert) {
            @Override public AlertDialog show() {
                AlertDialog dialog = create();
                dialog.setOnShowListener(ignored -> {
                    Typeface font=Typeface.createFromAsset(a.getAssets(), "fonts/Doto.ttf");
                    TextView message=dialog.findViewById(android.R.id.message); if (message != null) message.setTypeface(font);
                    int titleId=a.getResources().getIdentifier("alertTitle","id","android");
                    View title=titleId == 0 ? null : dialog.findViewById(titleId); if (title instanceof TextView) ((TextView)title).setTypeface(font);
                    for (int which : new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}) {
                        TextView button=dialog.getButton(which); if (button != null) button.setTypeface(font);
                    }
                    android.widget.ListView list=dialog.getListView();
                    if (list != null) list.post(() -> { for (int i=0;i<list.getChildCount();i++) { View row=list.getChildAt(i); if (row instanceof TextView) ((TextView)row).setTypeface(font); } });
                });
                dialog.show();
                if (a instanceof BottomSwipeActivity) VoiceController.watchDialog((BottomSwipeActivity)a, dialog);
                return dialog;
            }
        };
    }
    private int themeIcon(String value) { return "dark".equals(value) ? R.drawable.ic_calendar_theme_dark : "light".equals(value) ? R.drawable.ic_calendar_theme_light : R.drawable.ic_calendar_theme_system; }
    private ImageButton icon(Activity a, int resource, String description) {
        ImageButton button = new ImageButton(a); button.setImageResource(resource); button.setColorFilter(ink);
        button.setScaleType(ImageView.ScaleType.FIT_CENTER); button.setPadding(dp(a,12),dp(a,12),dp(a,12),dp(a,12));
        button.setBackgroundColor(Color.TRANSPARENT); button.setContentDescription(description); button.setTooltipText(description); return button;
    }
    /** Same dot-matrix arrow and 48 dp touch target as Calendar. */
    ImageButton backButton(Activity a) {
        ImageButton back = icon(a,R.drawable.ic_calendar_back,"Kembali");
        back.setOnClickListener(v -> a.finish()); return back;
    }
    LinearLayout header(Activity a, Typeface font, String title) {
        LinearLayout row = new LinearLayout(a); row.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton back=backButton(a);
        row.addView(back,new LinearLayout.LayoutParams(dp(a,48),dp(a,48)));
        TextView label=new TextView(a); label.setText(title); label.setTextSize(20); label.setTextColor(ink);
        label.setTypeface(font); label.setFontVariationSettings("'wght' 800"); label.setSingleLine(true); label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
        ImageButton theme=icon(a,themeIcon(choice),"Tema aplikasi: " + ("dark".equals(choice) ? "Gelap" : "light".equals(choice) ? "Terang" : "Ikuti perangkat"));
        row.addView(theme,new LinearLayout.LayoutParams(dp(a,48),dp(a,48)));
        theme.setOnClickListener(v -> {
            LinearLayout choices=new LinearLayout(a); choices.setGravity(Gravity.CENTER); choices.setPadding(dp(a,4),dp(a,4),dp(a,4),dp(a,4));
            PopupWindow popup=new PopupWindow(choices,-2,-2,true); popup.setBackgroundDrawable(surface(a,card)); popup.setOutsideTouchable(true); popup.setElevation(dp(a,6));
            for (String value : new String[]{"system","light","dark"}) {
                boolean active=value.equals(choice); String name="system".equals(value)?"Ikuti perangkat":"light".equals(value)?"Terang":"Gelap";
                ImageButton option=icon(a,themeIcon(value),"Tema " + name + (active ? ", dipilih" : ""));
                option.setColorFilter(active?selectedText:ink); option.setBackground(surface(a,active?selected:control)); option.setSelected(active);
                LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(a,48),dp(a,48)); size.setMargins(dp(a,2),0,dp(a,2),0); choices.addView(option,size);
                option.setOnClickListener(w -> { popup.dismiss(); if (!value.equals(choice)) { a.getSharedPreferences("calendar_appearance",Context.MODE_PRIVATE).edit().putString("theme",value).apply(); a.recreate(); } });
            }
            popup.showAsDropDown(theme,0,0,Gravity.END);
        });
        return row;
    }
}
