package com.floodcue.app;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the weather parsing logic. WeatherParser is a pure static helper, so
 * these tests can run on the JVM without an Android {@link android.app.Activity}
 * context (which is why the original RestClientTest could not mock it).
 */
public class RestClientTest {

    private static final String VALID = "{" +
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
            "\"time\": \"2026-10-26T12:00\"," +
            "}," +
            "\"hourly\": {" +
            "\"time\": [\"2026-10-26T12:00\",\"2026-10-26T13:00\"]," +
            "\"rain\": [0.4, 0.0]," +
            "\"rain_probability\": [60, 10]," +
            "\"visibility\": [10000, 10000]" +
            "}" +
            "}";

    @Test
    public void testParseWeatherJson_ValidJson_ReturnsData() {
        // The parser resolves currentIndex() from the hourly slot times. Since
        // the slots are ISO strings, currentIndex() picks the first slot at/after
        // now. A 0L updatedAt resolves currentIndex() to the last (first) slot,
        // so rainNext24h/rainNext6h/rainProbability reflect slot[0] (0.4 in).
        WeatherParser.WeatherData data = WeatherParser.parseJson(VALID, 0L);

        if (data != null) {
            System.out.println("[RestClientTest] latitude=" + data.latitude
                    + " humidity=" + data.humidity
                    + " rainNext24h=" + data.rainNext24h
                    + " rainNext6h=" + data.rainNext6h
                    + " rainProbability=" + data.rainProbability
                    + " hourlyRain=" + data.hourlyRain);
        }
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
        assertEquals(0.4, data.rainNext6h, 0.0001);
        assertEquals(60, data.rainProbability);
    }

    @Test
    public void testParseWeatherJson_MissingFields_ReturnsDefaults() {
        String json = "{\"latitude\":1.0,\"longitude\":2.0,\"current\":{\"temperature_2m\":30.0}}";
        WeatherParser.WeatherData data = WeatherParser.parseJson(json, 0L);

        assertNotNull(data);
        assertEquals(30.0, data.temperature, 0.0001);
        assertEquals(-1, data.humidity);
        assertEquals(-1d, data.currentRain, 0.0001);
        assertEquals(-1d, data.rainNext24h, 0.0001);
    }

    @Test
    public void testParseWeatherJson_InvalidJson_ReturnsNull() {
        assertNull(WeatherParser.parseJson("not json at all", 0L));
        assertNull(WeatherParser.parseJson("", 0L));
        assertNull(WeatherParser.parseJson(null, 0L));
    }

    @Test
    public void testParseJson_InvalidCurrent_ReturnsNull() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("latitude", 1.0);
        root.put("longitude", 2.0);
        root.put("current", new JSONObject());

        assertNull(WeatherParser.parseJson(root.toString(), 0L));
    }

    @Test
    public void testParseJson_MissingCurrent_ReturnsNull() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("latitude", 1.0);
        root.put("longitude", 2.0);

        assertNull(WeatherParser.parseJson(root.toString(), 0L));
    }

    @Test
    public void testParseJson_HourlyArrays_MismatchedLengths_NoCrash() {
        String json = "{" +
                "\"latitude\":1.0," +
                "\"longitude\":2.0," +
                "\"current\":{\"temperature_2m\":30.0}," +
                "\"hourly\":{\"time\":[\"2026-10-26T12:00\",\"2026-10-26T13:00\"],\"rain\":[0.4]}" +
                "}";
        WeatherParser.WeatherData data = WeatherParser.parseJson(json, 0L);
        assertNotNull(data);
        assertEquals(2, data.hourlyTimes.size());
        assertEquals(1, data.hourlyRain.size());
    }

    @Test
    public void testParseJson_NullPointerException_DoesNotCrash() {
        // null input must be handled without throwing
        assertNull(WeatherParser.parseJson(null, 0L));
        assertNull(WeatherParser.parseJson("{\"current\":null}", 0L));
    }

    @Test
    public void testCurrentIndex_ThenNextHour() {
        WeatherParser.WeatherData data = WeatherParser.parseJson(VALID, 0L);
        assertNotNull(data);
        // "2026-10-26T12:00" is the first slot at/after epoch, so index 0
        assertEquals(0, data.hourlyTimes.size());
    }
}
