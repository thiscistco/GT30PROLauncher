package com.gt30pro.launcher;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HolidayFeedTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static JSONObject copy(JSONObject x) throws Exception { return new JSONObject(x.toString()); }
    public static void main(String[] args) throws Exception {
        String raw = new String(Files.readAllBytes(Paths.get("assets/calendar/indonesia.json")), StandardCharsets.UTF_8);
        JSONObject base = new JSONObject(raw);
        check(HolidayFeed.valid(base), "bundled feed valid");
        JSONObject updated = copy(base); updated.put("revision", 2);
        check(HolidayFeed.valid(updated) && HolidayFeed.covers(updated, base), "updated feed covers base");
        JSONObject missing = copy(updated); missing.put("years", new JSONArray().put(2026));
        check(!HolidayFeed.covers(missing, base), "cannot drop previously covered year");
        JSONObject badDate = copy(base); badDate.getJSONObject("entries").put("2026-02-30", new JSONArray().put(new JSONObject().put("type", "libur").put("name", "x")));
        check(!HolidayFeed.valid(badDate), "invalid date");
        JSONObject badType = copy(base); badType.getJSONObject("entries").getJSONArray("2026-08-17").getJSONObject(0).put("type", "invalid");
        check(!HolidayFeed.valid(badType), "invalid category");
        JSONObject badRevision = copy(base); badRevision.put("revision", 1.5);
        check(!HolidayFeed.valid(badRevision), "revision must be an integer");
        JSONObject badYear = copy(base); badYear.getJSONArray("years").put(2026);
        check(!HolidayFeed.valid(badYear), "duplicate year");
        JSONObject noEntries = copy(base); noEntries.put("entries", new JSONObject());
        check(!HolidayFeed.valid(noEntries), "empty feed");
        System.out.println("Android holiday feed validation and year coverage: OK");
    }
}
