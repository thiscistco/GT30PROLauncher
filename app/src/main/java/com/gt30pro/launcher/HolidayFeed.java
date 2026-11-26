package com.gt30pro.launcher;

import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Bounded, validated, curated public feed; never reads or sends personal events. */
final class HolidayFeed {
    static final String URL_STRING = "https://api.github.com/repos/thiscistco/GT30PROLauncher/contents/assets/calendar/indonesia.json?ref=arena%2Ff57090c0-gt30prolauncher";
    static final int MAX_BYTES = 512 * 1024;
    static final long INTERVAL_MS = 12L * 60 * 60 * 1000;
    private HolidayFeed() {}

    static boolean valid(JSONObject data) {
        try {
            if (data == null || !(data.get("revision") instanceof Number)) return false;
            Number revision = (Number) data.get("revision");
            if (revision.longValue() < 1 || revision.longValue() > 9007199254740991L || revision.doubleValue() != revision.longValue()) return false;
            JSONArray years = data.getJSONArray("years");
            if (years.length() < 1 || years.length() > 100) return false;
            Set<Integer> allowed = new HashSet<>(), found = new HashSet<>();
            for (int i = 0; i < years.length(); i++) {
                Object value = years.get(i);
                if (!(value instanceof Number)) return false;
                int year = years.getInt(i);
                if (((Number) value).doubleValue() != year || year < 1900 || year > 2100 || !allowed.add(year)) return false;
            }
            JSONObject entries = data.getJSONObject("entries");
            if (entries.length() < 1 || entries.length() > 36600) return false;
            Iterator<String> keys = entries.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!key.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) return false;
                LocalDate date = LocalDate.parse(key);
                if (!date.toString().equals(key) || !allowed.contains(date.getYear())) return false;
                JSONArray events = entries.getJSONArray(key);
                if (events.length() < 1 || events.length() > 20) return false;
                for (int i = 0; i < events.length(); i++) {
                    JSONObject event = events.getJSONObject(i);
                    Object rawType = event.get("type"), rawName = event.get("name");
                    if (!(rawType instanceof String) || !(rawName instanceof String)) return false;
                    String type = (String) rawType, name = (String) rawName;
                    if (!("libur".equals(type) || "cuti".equals(type) || "peringatan".equals(type)) || name.trim().isEmpty() || name.length() > 160) return false;
                    if ("libur".equals(type)) found.add(date.getYear());
                }
            }
            return found.containsAll(allowed);
        } catch (Exception e) { return false; }
    }

    static boolean covers(JSONObject next, JSONObject previous) {
        try {
            JSONArray years = next.getJSONArray("years"), old = previous.getJSONArray("years");
            Set<Integer> set = new HashSet<>();
            for (int i = 0; i < years.length(); i++) set.add(years.getInt(i));
            for (int i = 0; i < old.length(); i++) if (!set.contains(old.getInt(i))) return false;
            return true;
        } catch (Exception e) { return false; }
    }

    static JSONObject fetch() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(URL_STRING).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "GT30PROLauncher-Calendar");
        connection.setUseCaches(false);
        try {
            if (connection.getResponseCode() != 200) throw new IllegalStateException("HTTP " + connection.getResponseCode());
            byte[] envelopeBytes;
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (out.size() + count > MAX_BYTES * 2) throw new IllegalStateException("Response too large");
                    out.write(buffer, 0, count);
                }
                envelopeBytes = out.toByteArray();
            }
            JSONObject envelope = new JSONObject(new String(envelopeBytes, StandardCharsets.UTF_8));
            if (!"base64".equals(envelope.getString("encoding")) || envelope.optLong("size", MAX_BYTES + 1) > MAX_BYTES) throw new IllegalStateException("Invalid envelope");
            byte[] decoded = Base64.decode(envelope.getString("content"), Base64.DEFAULT);
            if (decoded.length > MAX_BYTES) throw new IllegalStateException("Feed too large");
            JSONObject data = new JSONObject(new String(decoded, StandardCharsets.UTF_8));
            if (!valid(data)) throw new IllegalStateException("Invalid feed");
            return data;
        } finally { connection.disconnect(); }
    }
}
