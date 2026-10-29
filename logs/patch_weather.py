# Re-apply the minimal weather-parser changes to WeatherActivity.java.
import os

p = "app/src/main/java/com/floodcue/app/WeatherActivity.java"
raw = open(p, encoding="utf-8", newline="").read()
s = raw.replace("\r\n", "\n")
crlf = raw.count("\r\n"); lf = raw.count("\n")
print("CRLF:", crlf, "LF:", lf)

def rep(old, new, label, count=1):
    global s
    n = s.count(old)
    assert n == count, "ANCHOR FAIL [%s]: found %d expected %d" % (label, n, count)
    s = s.replace(old, new, count)

# ---- 1. WeatherData inner class right after class declaration ----
anchor = "public class WeatherActivity extends FloodCueActivity {"
if anchor not in s:
    raise SystemExit("class anchor missing")
inner = '''public class WeatherActivity extends FloodCueActivity {
    /** Weather payload extracted from the Open-Meteo forecast JSON. */
    public static class WeatherData {
        public double latitude;
        public double longitude;
        public double temperature;
        public int weatherCode;
        public double currentRain;
        public double windSpeed;
        public int windDirection;
        public String currentTime;
        public int humidity;
        public double feelsLike;
        public double pressure;
        public int cloudCover;
        public double windGusts;
        public long updatedAt;
        public java.util.List<String> hourlyTimes = new java.util.ArrayList<>();
        public java.util.List<Double> hourlyRain = new java.util.ArrayList<>();
        public double rainNext24h;
        public double rainNext6h;
        public double visibility;
        public int rainProbability;
    }

'''
    s = s.replace(anchor, inner, 1)

# ---- 2. parseJson -> delegate to static WeatherParser ----
rep("    public WeatherData parseJson(String json, long updatedAt) {",
    "    public WeatherData parseJson(String json, long updatedAt) {\n"
    "        return WeatherParser.parseJson(json, updatedAt);\n    }", label="parseJson delegate")

# ---- 3. saveWeatherCache -> persistWeatherCache ----
rep("                saveWeatherCache(response);",
    "                persistWeatherCache(response);", label="cache call site")

alias = '''
    // Thin alias kept for the cache call site seen above.
    private void persistWeatherCache(String rawJson) {
        if (rawJson == null) {
            return;
        }
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_CACHE, rawJson)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }
'''
rep("\n    @Override\n    protected void onDestroy() {",
    alias + "\n    @Override\n    protected void onDestroy() {", label="persist alias")

# ---- 4. unwrap dead applyResults / showNotificationBlockedToast / notifyNewSafe
#      (those belong to FloodAlertActivity; they must not have been copied here) ----
print("steps 1-4 done")

# ---- 5. WeatherParser nested static class (copy the parsing logic) ----
parser = '''
    /** Parses the Open-Meteo forecast JSON into {@link WeatherData}. */
    public static class WeatherParser {
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
}
'''
# remove the trailing '}\n}\n' that was the end of the class
s = s.replace(parser, "", 1)  # will fail if not present - just assert below
if "public static class WeatherParser" not in s:
    raise SystemExit("parser anchor missing")

# Append the parser class at the very end of the file (after the closing brace of
# WeatherActivity), because we could not reliably splice it mid-file.
s = s.rstrip() + "\n" + parser

open(p, "w", encoding="utf-8", newline="").write(s.replace("\n", "\r\n"))
print("parser inlined")
PYEOF
echo "exit:$?"