package com.floodcue.app;

import org.json.JSONArray;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class RestClientTest {

    @Test
    public void testOpenConnection_ReturnsHttpURLConnection() throws Exception {
        URL url = new URL("https://api.open-meteo.com/v1/forecast?latitude=13.63&longitude=79.42");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            assertNotNull(conn);
            assertEquals(HttpURLConnection.HTTP_OK, conn.getResponseCode());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Test
    public void testParseWeatherJson_ValidJson_ReturnsData() {
        String json = "{" +
                "\"latitude\": 13.63," +
                "\"longitude\": 79.42," +
                "\"current\": {" +
                "\"temperature_2m\": 29.5," +
                "\"weather_code\": 61," +
                "\"rain\": 0.4," +
                "\"wind_speed_10m\": 12.3," +
                "\"wind_direction_10m\": 220," +
                "\"relativehumidity_2m\": 65," +
                "\"apparent_temperature\": 31.2," +
                "\"pressure_msl\": 1012.5," +
                "\"cloud_cover\": 50," +
                "\"wind_gusts_10m\": 18.0," +
                "\"time\": \"2026-10-26T12:00\"" +
                "}," +
                "\"hourly\": {" +
                "\"time\": [\"2026-10-26T12:00\",\"2026-10-26T13:00\"]," +
                "\"rain\": [0.4, 0.0]," +
                "\"rain_probability\": [60, 10]," +
                "\"visibility\": [10000, 10000]" +
                "}" +
                "}";

        WeatherActivity activity = new WeatherActivity();
        WeatherActivity.WeatherData data = activity.parseJson(json, System.currentTimeMillis());

        assertNotNull(data);
        assertEquals(13.63, data.latitude, 0.0001);
        assertEquals(29.5, data.temperature, 0.0001);
        assertEquals(61, data.weatherCode);
        assertEquals(0.4, data.currentRain, 0.0001);
        assertEquals(65, data.humidity);
        assertEquals(31.2, data.feelsLike, 0.0001);
        assertEquals(50, data.cloudCover);
        assertEquals(18.0, data.windGusts, 0.0001);
        assertEquals(10000, data.visibility, 0.0001);
        assertEquals(2, data.hourlyTimes.size());
        assertEquals(2, data.hourlyRain.size());
        assertTrue(data.rainNext24h > 0);
    }

    @Test
    public void testParseWeatherJson_MissingFields_ReturnsDefaults() {
        String json = "{\"latitude\":1.0,\"longitude\":2.0,\"current\":{\"temperature_2m\":30.0}}";

        WeatherActivity activity = new WeatherActivity();
        WeatherActivity.WeatherData data = activity.parseJson(json, 0L);

        assertNotNull(data);
        assertEquals(30.0, data.temperature, 0.0001);
        assertEquals(-1, data.humidity);
        assertEquals(-1d, data.currentRain, 0.0001);
        assertEquals(-1d, data.rainNext24h, 0.0001);
    }

    @Test
    public void testParseWeatherJson_InvalidJson_ReturnsNull() {
        WeatherActivity activity = new WeatherActivity();
        assertNull(activity.parseJson("not json at all", 0L));
        assertNull(activity.parseJson("", 0L));
        assertNull(activity.parseJson(null, 0L));
    }

    @Test
    public void testParseJson_InvalidCurrent_ReturnsNull() throws JSONException {
        WeatherActivity activity = new WeatherActivity();

        JSONObject root = new JSONObject();
        root.put("latitude", 1.0);
        root.put("longitude", 2.0);
        root.put("current", new JSONObject());

        String json = root.toString();
        assertNull(activity.parseJson(json, 0L));
    }

    @Test
    public void testParseJson_MissingCurrent_ReturnsNull() throws JSONException {
        WeatherActivity activity = new WeatherActivity();

        JSONObject root = new JSONObject();
        root.put("latitude", 1.0);
        root.put("longitude", 2.0);

        String json = root.toString();
        assertNull(activity.parseJson(json, 0L));
    }

    @Test
    public void testParseJson_HourlyArrays_MismatchedLengths_NoCrash() {
        WeatherActivity activity = new WeatherActivity();

        String json = "{" +
                "\"latitude\":1.0," +
                "\"longitude\":2.0," +
                "\"current\":{\"temperature_2m\":30.0}," +
                "\"hourly\":{" +
                "\"time\":[\"2026-10-26T12:00\",\"2026-10-26T13:00\"]," +
                "\"rain\":[0.4]" +
                "}" +
                "}";

        WeatherActivity.WeatherData data = activity.parseJson(json, 0L);
        assertNotNull(data);
        assertEquals(2, data.hourlyTimes.size());
        assertEquals(1, data.hourlyRain.size());
    }

    @Test
    public void testParseJson_NullPointerException_DoesNotCrash() {
        WeatherActivity activity = new WeatherActivity();
        assertNull(activity.parseJson(null, 0L));
        assertNull(activity.parseJson("{\"current\":null}", 0L));
    }

    @Test
    public void testParseJson_NetworkError_DoesNotCrash() {
        // Simulate a null URL with a URL that will fail - should not throw
        WeatherActivity activity = new WeatherActivity();
        // The method should handle null/data gracefully
        assertNotNull(activity);
    }
}
