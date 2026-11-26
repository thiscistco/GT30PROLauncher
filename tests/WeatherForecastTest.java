package com.gt30pro.launcher;
import org.json.JSONArray;
import org.json.JSONObject;
public class WeatherForecastTest {
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        JSONObject current = new JSONObject().put("temperature_2m",29.4).put("weather_code",61).put("is_day",1).put("time","2026-11-24T12:30").put("latitude",-6.1);
        JSONArray hours = new JSONArray(); for (int i=0;i<220;i++) hours.put(i);
        JSONArray days = new JSONArray(); for (int i=0;i<9;i++) days.put(i);
        JSONObject input = new JSONObject().put("latitude",-6.1).put("longitude",110.1).put("current",current)
                .put("hourly",new JSONObject().put("temperature_2m",hours).put("latitude",hours))
                .put("daily",new JSONObject().put("temperature_2m_max",days));
        JSONObject out = WeatherForecast.sanitized(input);
        check(!out.has("latitude") && !out.has("longitude"));check(!out.getJSONObject("current").has("latitude"));check(!out.getJSONObject("hourly").has("latitude"));
        check(out.getJSONObject("current").getDouble("temperature_2m")==29.4);
        check(out.getJSONObject("hourly").getJSONArray("temperature_2m").length()==200);
        check(out.getJSONObject("daily").getJSONArray("temperature_2m_max").length()==7);
        JSONObject partial = WeatherForecast.sanitized(new JSONObject().put("current",current));check(!partial.has("hourly") && !partial.has("daily"));
        boolean invalid=false;try{WeatherForecast.sanitized(new JSONObject());}catch(Exception e){invalid=true;}check(invalid);
        System.out.println("Weather forecast sanitizer, missing data and coordinate privacy: OK");
    }
}
