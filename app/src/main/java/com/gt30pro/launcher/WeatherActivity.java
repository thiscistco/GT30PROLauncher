package com.gt30pro.launcher;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Location-based forecast page, using the same Doto and dark style as the launcher. */
public class WeatherActivity extends SwipeBackActivity {
    private int WHITE, MUTED, AMBER;
    private AppUi ui;
    private Typeface doto;
    private LinearLayout page, details;
    private TextView status, temperature, condition, observed, location;
    private WeatherView hero;
    private WeatherController weather;
    @Override protected boolean supportsBottomSwipe() { return true; }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ui = new AppUi(this); WHITE=ui.ink; MUTED=ui.muted; AMBER=ui.ink; ui.window(this);
        doto = Typeface.createFromAsset(getAssets(), "fonts/Doto.ttf");
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(ui.canvas); scroll.setFillViewport(true);
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(dp(20),dp(16),dp(20),dp(28));
        scroll.addView(page, new ScrollView.LayoutParams(-1,-2));
        page.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(dp(20), dp(16)+insets.getSystemWindowInsetTop(), dp(20), dp(28)+insets.getSystemWindowInsetBottom()); return insets;
        });
        setContentView(scroll);
        page.addView(ui.header(this, doto, "Cuaca"));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); add(page,header,0,8,0,0);
        TextView title = text("CUACA SAAT INI", 16, WHITE); header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        ImageButton refresh = new ImageButton(this); refresh.setImageResource(R.drawable.ic_weather_refresh);
        refresh.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER); refresh.setPadding(dp(12),dp(12),dp(12),dp(12));
        refresh.setBackground(ui.iconGlass(this,false)); refresh.setColorFilter(ui.ink); refresh.setContentDescription("Aktifkan / perbarui cuaca");
        refresh.setOnClickListener(v -> weather.requestFromUser()); header.addView(refresh,new LinearLayout.LayoutParams(dp(48),dp(48)));
        location = text("LOKASI SAAT INI · PERKIRAAN", 11, MUTED); add(page,location,0,4,0,0);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(14),dp(14),dp(14),dp(14)); top.setBackground(new WeatherSkyDrawable(this)); top.setClipToOutline(true); add(page,top,0,14,0,0);
        LinearLayout summary = new LinearLayout(this); summary.setOrientation(LinearLayout.VERTICAL);
        top.addView(summary,new LinearLayout.LayoutParams(0,-2,1));
        temperature=text("--°",48,0xFFF5F5F5); summary.addView(temperature);
        condition=text("Cuaca belum tersedia",14,0xFFFFD49D); add(summary,condition,0,4,0,0);
        observed=text("",10,0xFFE5ECF2); add(summary,observed,0,5,0,0);
        hero=new WeatherView(this,false); top.addView(hero,new LinearLayout.LayoutParams(dp(90),dp(90)));
        status=text("Aktifkan lokasi untuk memuat cuaca.",11,MUTED); add(page,status,0,10,0,0);
        details = new LinearLayout(this); details.setOrientation(LinearLayout.VERTICAL); add(page,details,0,22,0,0);
        weather = new WeatherController(this, hero); weather.setDetailsListener(this::render);
        render(null, "Ketuk ikon di sebelah judul untuk mengaktifkan lokasi.", null);
    }
    @Override protected void onResume() { super.onResume(); AppUi current=new AppUi(this); if (ui.dark != current.dark || !ui.choice.equals(current.choice)) { recreate(); return; } if (weather != null) weather.resume(); }
    @Override protected void onPause() { if (weather != null) weather.pause(); super.onPause(); }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) { super.onRequestPermissionsResult(code, permissions, results); if (weather != null) weather.permissionResult(code); }

    private void render(JSONObject data, String message, String place) {
        location.setText(place == null || place.isEmpty() ? "LOKASI SAAT INI · PERKIRAAN" : place);
        status.setText(message); details.removeAllViews();
        JSONObject current = data == null ? null : data.optJSONObject("current");
        if (current == null) {
            temperature.setText("--°"); condition.setText("Cuaca belum tersedia"); observed.setText("");
            TextView empty=text("Prakiraan per jam dan 7 hari akan muncul setelah lokasi dan data cuaca tersedia.",13,MUTED); empty.setLineSpacing(dp(4),1); details.addView(empty); return;
        }
        temperature.setText(number(current,"temperature_2m","°"));
        int code = current.optInt("weather_code", -1); boolean day = current.optInt("is_day",1)==1;
        condition.setText(WeatherIcons.description(code,day));
        observed.setText("Waktu data: " + readableTime(current.optString("time", "")));
        heading("PRAKIRAAN 24 JAM");
        JSONObject hourly=data.optJSONObject("hourly");
        JSONArray times=hourly==null?null:hourly.optJSONArray("time");
        int start=findHour(times,current.optString("time", ""));
        if (start < 0) add(details,text("Data per jam belum tersedia.",12,MUTED),0,12,0,0);
        else {
            HorizontalScrollView hscroll=new HorizontalScrollView(this); hscroll.setHorizontalScrollBarEnabled(false); hscroll.setBackground(ui.surface(this,ui.card)); hscroll.setPadding(dp(4),dp(8),dp(4),dp(8));
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); hscroll.addView(row);
            for (int i=start;i<Math.min(start+24,times.length());i++) {
                LinearLayout column=new LinearLayout(this); column.setGravity(Gravity.CENTER); column.setOrientation(LinearLayout.VERTICAL);
                row.addView(column,new LinearLayout.LayoutParams(dp(66),-2));
                TextView hour=text(hourOf(times.optString(i,"")),12,MUTED); hour.setGravity(Gravity.CENTER); column.addView(hour);
                WeatherView icon=smallIcon(arrayInt(hourly,"weather_code",i,-1),arrayInt(hourly,"is_day",i,1)==1);
                column.addView(icon,new LinearLayout.LayoutParams(dp(48),dp(48)));
                TextView t=text(arrayNumber(hourly,"temperature_2m",i,"°"),14,WHITE);t.setGravity(Gravity.CENTER);column.addView(t);
                TextView rain=text(arrayNumber(hourly,"precipitation_probability",i,"%"),10,MUTED);rain.setGravity(Gravity.CENTER);add(column,rain,0,4,0,0);
            }
            add(details,hscroll,0,10,0,0);
            add(details,text("% = peluang hujan",10,MUTED),0,5,0,0);
        }
        heading("PRAKIRAAN 7 HARI");
        JSONObject daily=data.optJSONObject("daily"); JSONArray days=daily==null?null:daily.optJSONArray("time");
        if (days == null || days.length()==0) add(details,text("Data harian belum tersedia.",12,MUTED),0,12,0,0);
        else for(int i=0;i<Math.min(days.length(),7);i++) {
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),0,dp(8),0);row.setBackground(ui.surface(this,ui.card));add(details,row,0,6,0,0);
            TextView date=text(dayOf(days.optString(i,""),i),13,WHITE);row.addView(date,new LinearLayout.LayoutParams(0,dp(42),1));date.setGravity(Gravity.CENTER_VERTICAL);
            row.addView(smallIcon(arrayInt(daily,"weather_code",i,-1),true),new LinearLayout.LayoutParams(dp(42),dp(42)));
            TextView rain=text(arrayNumber(daily,"precipitation_probability_max",i,"%"),11,MUTED);rain.setGravity(Gravity.CENTER);row.addView(rain,new LinearLayout.LayoutParams(dp(50),dp(42)));
            TextView range=text(arrayNumber(daily,"temperature_2m_max",i,"°")+" / "+arrayNumber(daily,"temperature_2m_min",i,"°"),12,WHITE);range.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);row.addView(range,new LinearLayout.LayoutParams(dp(86),dp(42)));
        }
        heading("DETAIL SAAT INI");
        detailRow("TERASA",number(current,"apparent_temperature","°"));
        detailRow("KELEMBAPAN",number(current,"relative_humidity_2m","%"));
        detailRow("ANGIN",number(current,"wind_speed_10m"," km/j"));
        detailRow("TEKANAN",number(current,"surface_pressure"," hPa"));
        detailRow("MATAHARI TERBIT",hourOf(arrayString(daily,"sunrise",0)));
        detailRow("MATAHARI TERBENAM",hourOf(arrayString(daily,"sunset",0)));
        add(details,text("Sumber: Open-Meteo · waktu sesuai lokasi perkiraan",10,MUTED),0,18,0,0);
    }
    private int findHour(JSONArray times,String now) {
        if (times == null || now.length()<13) return -1;
        String hour=now.substring(0,13);
        for(int i=0;i<times.length();i++) { String t=times.optString(i,""); if(t.length()>=13 && t.substring(0,13).compareTo(hour)>=0) return i; }
        return -1;
    }
    private WeatherView smallIcon(int code, boolean day) { WeatherView icon=new WeatherView(this,false); icon.setIconColor(ui.ink); icon.setWeather(code,day,null,"");icon.setClickable(false);icon.setFocusable(false);return icon; }
    private void heading(String value) { TextView v=text(value,14,AMBER);add(details,v,0,24,0,0); }
    private void detailRow(String key,String value) { LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);add(details,row,0,10,0,0);TextView label=text(key,11,MUTED);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));row.addView(text(value,13,WHITE)); }
    private static String number(JSONObject o,String key,String suffix) { if(o==null||o.isNull(key))return "--"; double n=o.optDouble(key,Double.NaN);return Double.isFinite(n)?Math.round(n)+suffix:"--"; }
    private static String arrayNumber(JSONObject o,String key,int i,String suffix) { JSONArray a=o==null?null:o.optJSONArray(key); if(a==null||i>=a.length()||a.isNull(i))return "--";double n=a.optDouble(i,Double.NaN);return Double.isFinite(n)?Math.round(n)+suffix:"--"; }
    private static int arrayInt(JSONObject o,String key,int i,int fallback) { JSONArray a=o==null?null:o.optJSONArray(key);return a==null||i>=a.length()?fallback:a.optInt(i,fallback); }
    private static String arrayString(JSONObject o,String key,int i) { JSONArray a=o==null?null:o.optJSONArray(key);return a==null||i>=a.length()?"":a.optString(i,""); }
    private static String hourOf(String time) { return time != null && time.length()>=16 ? time.substring(11,16) : "--:--"; }
    private static String readableTime(String t) { return t != null && t.length()>=16 ? t.substring(0,10)+" "+t.substring(11,16) : "--"; }
    private static String dayOf(String value,int index) { try { return index==0?"Hari ini":LocalDate.parse(value).format(DateTimeFormatter.ofPattern("EEE, d MMM",new Locale("id","ID"))); } catch(Exception ex){return "--";} }
    private TextView text(String value,int size,int color) { TextView t=new TextView(this);t.setTypeface(doto);t.setFontVariationSettings("'wght' 800");t.setText(value);t.setTextSize(size);t.setTextColor(color);return t; }
    private void add(LinearLayout parent, View child,int l,int t,int r,int b) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));parent.addView(child,p); }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
}
