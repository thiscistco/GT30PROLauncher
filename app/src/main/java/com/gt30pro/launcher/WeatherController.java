package com.gt30pro.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import org.json.JSONObject;

/** Opt-in foreground-only approximate location and Open-Meteo current weather. */
final class WeatherController {
    static final int REQUEST_LOCATION = 812;
    private static final long INTERVAL = 30 * 60 * 1000L;
    private final Activity activity;
    private final WeatherView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LocationManager manager;
    private boolean active, loading, pendingManual;
    private int generation;
    private LocationListener listener;
    private Runnable timeout;
    private final Runnable scheduled = () -> refresh(false);
    WeatherController(Activity a, WeatherView v) {
        activity = a; view = v; manager = (LocationManager) a.getSystemService(Context.LOCATION_SERVICE);
        view.setOnClickListener(w -> requestFromUser());
    }
    private boolean permitted() { return activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED; }
    void resume() {
        active = true;
        if (!permitted()) { view.setWeather(-1, true, null, "Ketuk untuk mengaktifkan cuaca"); return; }
        showCached();
        boolean manual = pendingManual; pendingManual = false;
        refresh(manual);
    }
    void pause() {
        active = false; generation++; loading = false;
        handler.removeCallbacks(scheduled); stopLocation();
    }
    void permissionResult(int request) {
        if (request != REQUEST_LOCATION) return;
        if (permitted()) {
            if (active) { pendingManual = false; refresh(true); }
            else pendingManual = true;
        }
        else { pendingManual = false; view.setWeather(-1, true, null, "Izin lokasi ditolak; ketuk untuk mencoba lagi"); toast("Izin lokasi diperlukan untuk cuaca."); }
    }
    void requestFromUser() {
        if (permitted()) { refresh(true); return; }
        new AlertDialog.Builder(activity).setTitle("Aktifkan cuaca?")
                .setMessage("Lokasi perkiraan diperlukan. Koordinat perkiraan dikirim ke Open-Meteo untuk mengambil cuaca. Lokasi tidak dipantau di latar belakang.")
                .setNegativeButton("Batal", null).setPositiveButton("Lanjutkan", (d, w) -> {
                    if (activity.getSharedPreferences("weather_cache", Context.MODE_PRIVATE).getBoolean("weather_asked", false)
                            && !activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        new AlertDialog.Builder(activity).setMessage("Jika izin lokasi diblokir, aktifkan lewat setelan aplikasi.")
                                .setNegativeButton("Batal", null).setPositiveButton("Setelan", (dlg, which) -> activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName())))).show();
                    } else {
                        activity.getSharedPreferences("weather_cache", Context.MODE_PRIVATE).edit().putBoolean("weather_asked", true).apply();
                        pendingManual = true;
                        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                    }
                }).show();
    }
    private void showCached() {
        android.content.SharedPreferences p = activity.getSharedPreferences("weather_cache", Context.MODE_PRIVATE);
        long at = p.getLong("time", 0);
        long age = System.currentTimeMillis() - at;
        if (at > 0 && age >= 0 && age < 2 * 60 * 60 * 1000L) {
            view.setWeather(p.getInt("code", -1), p.getBoolean("day", true), p.getInt("temp", 0), "Data terakhir; ketuk untuk memperbarui");
        } else view.setWeather(-1, true, null, "Ketuk untuk memperbarui");
    }
    private void refresh(boolean manual) {
        if (!active || !permitted()) return;
        if (loading) { if (manual) toast("Cuaca sedang diperbarui…"); return; }
        android.content.SharedPreferences p = activity.getSharedPreferences("weather_cache", Context.MODE_PRIVATE);
        long age = System.currentTimeMillis() - p.getLong("time", 0);
        if (!manual && age >= 0 && age < INTERVAL) { schedule(INTERVAL-age); return; }
        loading = true; final int run = ++generation;
        if (manual) toast("Memperbarui cuaca…");
        try {
            if (manager == null) throw new IllegalStateException();
            // A recent passive fix can exist even when the network provider has
            // no cached fix. It is still filtered by Android's coarse permission.
            Location last = recentLocation();
            if (last != null) { fetch(last, run, manual); return; }
            if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) throw new IllegalStateException();
            listener = new LocationListener() {
                @Override public void onLocationChanged(Location location) { stopLocation(); fetch(location, run, manual); }
                @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
                @Override public void onProviderEnabled(String provider) { }
                @Override public void onProviderDisabled(String provider) { fail(run, manual, "Aktifkan lokasi perangkat."); }
            };
            manager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper());
            timeout = () -> fail(run, manual, "Lokasi belum tersedia. Coba lagi.");
            handler.postDelayed(timeout, 20000);
        } catch (SecurityException | IllegalStateException | IllegalArgumentException ex) { fail(run, manual, "Aktifkan lokasi perangkat dan izinkan lokasi perkiraan."); }
    }
    private Location recentLocation() {
        Location best = null;
        for (String provider : new String[]{LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER}) {
            try {
                Location candidate = manager.getLastKnownLocation(provider);
                long age = candidate == null ? -1 : System.currentTimeMillis() - candidate.getTime();
                if (candidate != null && age >= 0 && age < 10 * 60 * 1000L
                        && (best == null || candidate.getTime() > best.getTime())) best = candidate;
            } catch (IllegalArgumentException | SecurityException ignored) { /* Provider absent or unavailable. */ }
        }
        return best;
    }
    private void fetch(Location loc, int run, boolean manual) {
        // Round before sending; do not persist coordinates.
        final double lat = Math.round(loc.getLatitude()*100d)/100d, lon = Math.round(loc.getLongitude()*100d)/100d;
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String url = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code,is_day", lat, lon);
                conn = (HttpURLConnection) new URL(url).openConnection(); conn.setConnectTimeout(12000); conn.setReadTimeout(12000);
                if (conn.getResponseCode() != 200) throw new java.io.IOException("weather response");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream input = conn.getInputStream()) { byte[] buf = new byte[2048]; int n; while ((n=input.read(buf)) != -1) { out.write(buf,0,n); if(out.size()>65536) throw new java.io.IOException("weather response too large"); } }
                JSONObject current = new JSONObject(out.toString("UTF-8")).getJSONObject("current");
                double value = current.getDouble("temperature_2m"); int code = current.getInt("weather_code"), isDay = current.getInt("is_day");
                if (!Double.isFinite(value) || value < -100 || value > 70 || code < 0 || code > 99 || (isDay != 0 && isDay != 1)) throw new IllegalArgumentException();
                int temp = (int)Math.round(value); boolean day = isDay == 1;
                handler.post(() -> {
                    if (!active || run != generation || !permitted()) return;
                    loading = false; activity.getSharedPreferences("weather_cache", Context.MODE_PRIVATE).edit().putLong("time",System.currentTimeMillis()).putInt("temp",temp).putInt("code",code).putBoolean("day",day).apply();
                    view.setWeather(code, day, temp, "Diperbarui; ketuk untuk memperbarui lagi"); schedule(INTERVAL);
                });
            } catch (Exception e) { handler.post(() -> fail(run, manual, "Cuaca tidak tersedia. Periksa koneksi dan coba lagi.")); }
            finally { if (conn != null) conn.disconnect(); }
        }, "weather-fetch").start();
    }
    private void fail(int run, boolean manual, String message) {
        if (!active || run != generation) return;
        stopLocation(); loading = false; showCached(); schedule(INTERVAL);
        if (manual) toast(message);
    }
    private void stopLocation() {
        if (timeout != null) { handler.removeCallbacks(timeout); timeout = null; }
        if (listener != null && manager != null) { try { manager.removeUpdates(listener); } catch (SecurityException ignored) { } listener = null; }
    }
    private void schedule(long delay) { handler.removeCallbacks(scheduled); if (active) handler.postDelayed(scheduled, Math.max(1000, delay)); }
    private void toast(String s) { Toast.makeText(activity, s, Toast.LENGTH_LONG).show(); }
}
