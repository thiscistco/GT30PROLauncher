package com.gt30pro.launcher;

import org.json.JSONArray;
import org.json.JSONObject;

/** Keep only requested weather fields; coordinates are never persisted. */
final class WeatherForecast {
    private static final String[] CURRENT = {"time", "temperature_2m", "relative_humidity_2m", "apparent_temperature", "weather_code", "is_day", "wind_speed_10m", "surface_pressure"};
    private static final String[] HOURLY = {"time", "temperature_2m", "weather_code", "precipitation_probability", "is_day"};
    private static final String[] DAILY = {"time", "weather_code", "temperature_2m_max", "temperature_2m_min", "sunrise", "sunset", "precipitation_probability_max"};
    static JSONObject sanitized(JSONObject input) throws Exception {
        JSONObject out = new JSONObject();
        JSONObject current = input.getJSONObject("current");
        JSONObject safeCurrent = new JSONObject();
        for (String key : CURRENT) if (current.has(key)) safeCurrent.put(key, current.get(key));
        out.put("current", safeCurrent);
        copyArrays(input, out, "hourly", HOURLY, 200);
        copyArrays(input, out, "daily", DAILY, 7);
        return out;
    }
    private static void copyArrays(JSONObject input, JSONObject out, String name, String[] keys, int max) throws Exception {
        JSONObject source = input.optJSONObject(name);
        if (source == null) return;
        JSONObject safe = new JSONObject();
        for (String key : keys) {
            JSONArray values = source.optJSONArray(key);
            if (values == null) continue;
            JSONArray copy = new JSONArray();
            for (int i = 0; i < Math.min(values.length(), max); i++) copy.put(values.get(i));
            safe.put(key, copy);
        }
        out.put(name, safe);
    }
    private WeatherForecast() { }
}
