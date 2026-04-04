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

public class WeatherRepository {

    // Checks whether a usable OpenWeather API key is configured.
    public boolean hasApiKey() {
        String key = ApiConfig.OPEN_WEATHER_API_KEY;
        return key != null && !key.trim().isEmpty() && !key.startsWith("YOUR_");
    }

    // Loads current weather from OpenWeather for a given location.
    public WeatherInfo loadCurrentWeather(double latitude, double longitude) throws Exception {
        if (!hasApiKey()) {
            throw new IllegalStateException("OpenWeather API key missing");
        }

        String urlText = buildWeatherUrl(latitude, longitude);

        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("Weather request failed with status " + responseCode);
            }

            String responseText = readResponseText(connection);

            JSONObject response = new JSONObject(responseText);
            JSONObject mainObject = response.optJSONObject("main");
            JSONArray weatherArray = response.optJSONArray("weather");

            double temperature = mainObject == null ? Double.NaN : mainObject.optDouble("temp", Double.NaN);
            String condition = parseCondition(weatherArray);

            return new WeatherInfo(temperature, capitalizeWords(condition));
        } finally {
            connection.disconnect();
        }
    }

    // Loads upcoming hourly forecast points (OpenWeather 3-hour intervals).
    public List<WeatherHourlyInfo> loadHourlyWeather(double latitude, double longitude, int maxItems) throws Exception {
        if (!hasApiKey()) {
            throw new IllegalStateException("OpenWeather API key missing");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(buildForecastUrl(latitude, longitude)).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("Weather forecast request failed with status " + responseCode);
            }

            JSONObject response = new JSONObject(readResponseText(connection));
            JSONArray list = response.optJSONArray("list");
            List<WeatherHourlyInfo> hourly = new ArrayList<>();
            if (list == null) {
                return hourly;
            }

            int count = Math.min(Math.max(maxItems, 0), list.length());
            for (int index = 0; index < count; index++) {
                JSONObject item = list.optJSONObject(index);
                if (item == null) {
                    continue;
                }

                JSONObject mainObject = item.optJSONObject("main");
                JSONArray weatherArray = item.optJSONArray("weather");

                double temperature = mainObject == null ? Double.NaN : mainObject.optDouble("temp", Double.NaN);
                String condition = capitalizeWords(parseCondition(weatherArray));
                String hourLabel = extractHourLabel(item.optString("dt_txt", ""));

                hourly.add(new WeatherHourlyInfo(hourLabel, temperature, condition));
            }
            return hourly;
        } finally {
            connection.disconnect();
        }
    }

    // Builds the full OpenWeather URL with metric units.
    private String buildWeatherUrl(double latitude, double longitude) {
        return ApiConfig.OPEN_WEATHER_API_BASE
                + "?lat=" + latitude
                + "&lon=" + longitude
                + "&units=metric"
                + "&appid=" + ApiConfig.OPEN_WEATHER_API_KEY;
    }

    // Builds a forecast URL by reusing configured base endpoint.
    private String buildForecastUrl(double latitude, double longitude) {
        String forecastBase = ApiConfig.OPEN_WEATHER_API_BASE;
        if (forecastBase.endsWith("/weather")) {
            forecastBase = forecastBase.substring(0, forecastBase.length() - "/weather".length()) + "/forecast";
        }
        return forecastBase
                + "?lat=" + latitude
                + "&lon=" + longitude
                + "&units=metric"
                + "&appid=" + ApiConfig.OPEN_WEATHER_API_KEY;
    }

    // Reads HTTP response body text from the weather request.
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

    // Pulls the first weather condition description from the JSON array.
    private String parseCondition(JSONArray weatherArray) throws Exception {
        if (weatherArray == null || weatherArray.length() == 0) {
            return "Unknown";
        }
        JSONObject firstWeather = weatherArray.getJSONObject(0);
        return firstWeather.optString("description", firstWeather.optString("main", "Unknown"));
    }

    // Extracts HH:mm from OpenWeather dt_txt, falls back to "Soon".
    private String extractHourLabel(String rawDateTime) {
        if (rawDateTime != null && rawDateTime.length() >= 16) {
            return rawDateTime.substring(11, 16);
        }
        return "Soon";
    }

    // Formats condition words with uppercase initials for display.
    private String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) {
            return "Unknown";
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
