package com.floodcue.app;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Pure JSON parsing helper for the Open-Meteo forecast response. Kept separate
 * from {@link WeatherActivity} so the parsing logic can be unit-tested on the
 * JVM without an Android {@link android.app.Activity} context.
 */
public final class WeatherParser {

    private WeatherParser() {
    }

    /** Weather data extracted from the API JSON. */
    public static class WeatherData extends com.floodcue.app.WeatherActivity.WeatherData {
        public WeatherData() {
            super();
        }
    }

    private static final int FORECAST_HOURS = 24;

    public static WeatherData parseJson(String json, long updatedAt) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONObject current = root.optJSONObject("current");
            if (current == null || !current.has("temperature_2m")) {
                return null;
            }

            WeatherData data = new WeatherData();
            data.latitude = root.optDouble("latitude", 0.0);
            data.longitude = root.optDouble("longitude", 0.0);
            data.temperature = current.optDouble("temperature_2m", 0.0);
            data.weatherCode = current.optInt("weather_code", -1);
            data.currentRain = current.has("rain") ? current.optDouble("rain", 0.0) : -1d;
            data.windSpeed = current.has("wind_speed_10m")
                    ? current.optDouble("wind_speed_10m", 0.0) : -1d;
            data.windDirection = current.has("wind_direction_10m")
                    ? current.optInt("wind_direction_10m", -1) : -1;
            data.currentTime = current.optString("time", "");
            data.humidity = current.has("relativehumidity_2m")
                    ? current.optInt("relativehumidity_2m", -1) : -1;
            data.feelsLike = current.has("apparent_temperature")
                    ? current.optDouble("apparent_temperature", -1d) : -1d;
            data.pressure = current.has("pressure_msl")
                    ? current.optDouble("pressure_msl", -1d) : -1d;
            data.cloudCover = current.has("cloud_cover")
                    ? current.optInt("cloud_cover", -1) : -1;
            data.windGusts = current.has("wind_gusts_10m")
                    ? current.optDouble("wind_gusts_10m", -1d) : -1d;
            data.updatedAt = updatedAt;

            JSONObject hourly = root.optJSONObject("hourly");
            JSONArray times = hourly == null ? null : hourly.optJSONArray("time");
            JSONArray rains = hourly == null ? null : hourly.optJSONArray("rain");
            JSONArray probabilities =
                    hourly == null ? null : hourly.optJSONArray("rain_probability");
            JSONArray visibility =
                    hourly == null ? null : hourly.optJSONArray("visibility");
            if (times != null) {
                for (int i = 0; i < times.length(); i++) {
                    data.hourlyTimes.add(times.optString(i, ""));
                }
            }
            if (rains != null) {
                for (int i = 0; i < rains.length(); i++) {
                    data.hourlyRain.add(rains.optDouble(i, 0.0));
                }
            }

            int now = currentIndex(data);
            data.rainNext24h = 0d;
            data.rainNext6h = -1d;
            data.visibility = -1d;
            data.rainProbability = -1;
            if (now >= 0) {
                int end = Math.min(now + FORECAST_HOURS, data.hourlyRain.size());
                for (int i = now; i < end; i++) {
                    data.rainNext24h += data.hourlyRain.get(i);
                }
                int soon = Math.min(now + 6, data.hourlyRain.size());
                data.rainNext6h = 0d;
                for (int i = now; i < soon; i++) {
                    data.rainNext6h += data.hourlyRain.get(i);
                }
                if (probabilities != null && now < probabilities.length()) {
                    data.rainProbability = probabilities.optInt(now, -1);
                }
                if (visibility != null && now < visibility.length()) {
                    data.visibility = visibility.optDouble(now, -1d);
                }
            }
            return data;
        } catch (Throwable t) {
            // Never let a bad payload kill the UI thread; return null instead.
            return null;
        }
    }

    /** Index of the first hourly slot whose time is at/after now, or -1. */
    private static int currentIndex(WeatherData data) {
        long now = System.currentTimeMillis() / 1000;
        for (int i = 0; i < data.hourlyTimes.size(); i++) {
            String t = data.hourlyTimes.get(i);
            try {
                long slot = Long.parseLong(t);
                if (slot >= now) {
                    return i;
                }
            } catch (NumberFormatException ignored) {
                // Non-numeric slot names (e.g. "12:00") - skip; the next
                // numeric or the last slot still bounds the 24h totals.
            }
        }
        return data.hourlyTimes.isEmpty() ? -1 : data.hourlyTimes.size() - 1;
    }
}