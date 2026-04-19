package com.reserve.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WeatherApiClient {

    private static final String UNKNOWN_TEXT = "Unknown";
    private static final String SOON_TEXT = "Soon";
    private static final String WEATHER_PATH_SUFFIX = "/weather";

    public boolean hasApiKey() {
        String key = BuildConfig.OPEN_WEATHER_API_KEY;
        return key != null && !key.trim().isEmpty() && !key.startsWith("YOUR_");
    }

    public WeatherCurrent loadCurrentWeather(double latitude, double longitude) throws Exception {
        requireApiKey();

        JSONObject response = readJson(buildWeatherUrl(latitude, longitude), "Weather request failed with status");
        return new WeatherCurrent(
                readTemperature(response.optJSONObject("main")),
                capitalizeWords(parseCondition(response.optJSONArray("weather")))
        );
    }

    public List<WeatherHourlyForecast> loadHourlyWeather(double latitude, double longitude, int maxItems) throws Exception {
        requireApiKey();

        JSONArray list = readJson(buildForecastUrl(latitude, longitude), "Weather forecast request failed with status")
                .optJSONArray("list");
        List<WeatherHourlyForecast> hourly = new ArrayList<>();
        if (list == null) {
            return hourly;
        }

        int count = Math.min(Math.max(maxItems, 0), list.length());
        for (int index = 0; index < count; index++) {
            JSONObject item = list.optJSONObject(index);
            if (item != null) {
                hourly.add(buildHourlyInfo(item));
            }
        }
        return hourly;
    }

    private String buildWeatherUrl(double latitude, double longitude) {
        return BuildConfig.OPEN_WEATHER_API_BASE + buildQueryParameters(latitude, longitude);
    }

    private String buildForecastUrl(double latitude, double longitude) {
        String forecastBase = BuildConfig.OPEN_WEATHER_API_BASE;
        if (forecastBase.endsWith(WEATHER_PATH_SUFFIX)) {
            forecastBase = forecastBase.substring(0, forecastBase.length() - WEATHER_PATH_SUFFIX.length()) + "/forecast";
        }
        return forecastBase + buildQueryParameters(latitude, longitude);
    }

    private String buildQueryParameters(double latitude, double longitude) {
        return "?lat=" + latitude
                + "&lon=" + longitude
                + "&units=metric"
                + "&appid=" + BuildConfig.OPEN_WEATHER_API_KEY;
    }

    private void requireApiKey() {
        if (!hasApiKey()) {
            throw new IllegalStateException("OpenWeather API key missing");
        }
    }

    private String parseCondition(JSONArray weatherArray) throws Exception {
        if (weatherArray == null || weatherArray.length() == 0) {
            return UNKNOWN_TEXT;
        }
        JSONObject firstWeather = weatherArray.getJSONObject(0);
        return firstWeather.optString("description", firstWeather.optString("main", UNKNOWN_TEXT));
    }

    private double readTemperature(JSONObject mainObject) {
        return mainObject == null ? Double.NaN : mainObject.optDouble("temp", Double.NaN);
    }

    private WeatherHourlyForecast buildHourlyInfo(JSONObject item) throws Exception {
        return new WeatherHourlyForecast(
                extractHourLabel(item.optString("dt_txt", "")),
                readTemperature(item.optJSONObject("main")),
                capitalizeWords(parseCondition(item.optJSONArray("weather")))
        );
    }

    private JSONObject readJson(String url, String errorPrefix) throws Exception {
        HttpURLConnection connection = openJsonGetConnection(url);
        try {
            requireSuccessResponse(connection, errorPrefix);
            return new JSONObject(readResponseText(connection));
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openJsonGetConnection(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private void requireSuccessResponse(HttpURLConnection connection, String errorPrefix) throws Exception {
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException(errorPrefix + " " + responseCode);
        }
    }

    private String readResponseText(HttpURLConnection connection) throws Exception {
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private String extractHourLabel(String rawDateTime) {
        if (rawDateTime != null && rawDateTime.length() >= 16) {
            return rawDateTime.substring(11, 16);
        }
        return SOON_TEXT;
    }

    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) {
            return UNKNOWN_TEXT;
        }

        String[] parts = text.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
