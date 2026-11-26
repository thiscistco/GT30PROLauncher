package com.gt30pro.launcher;

/** Coarse place labels without street-level addresses. */
final class WeatherLocation {
    static String label(String locality, String district, String region) {
        String place = clean(locality), districtName = clean(district), regionName = clean(region);
        if (place.isEmpty()) place = districtName;
        if (place.isEmpty()) place = regionName;
        if (place.isEmpty()) return "";
        if (!regionName.isEmpty() && !regionName.equalsIgnoreCase(place)) return place + ", " + regionName;
        return place;
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private WeatherLocation() { }
}
