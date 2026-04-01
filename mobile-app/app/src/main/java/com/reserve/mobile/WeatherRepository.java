package com.reserve.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class WeatherRepository {

    public boolean hasApiKey() {
        return ApiConfig.OPEN_WEATHER_API_KEY != null && !ApiConfig.OPEN_WEATHER_API_KEY.isEmpty();
    }

    public WeatherInfo loadCurrentWeather(double latitude, double longitude) throws Exception {
        if (!hasApiKey()) {
            throw new IllegalStateException("OpenWeather API key missing");
        }

        String urlText = ApiConfig.OPEN_WEATHER_API_BASE
                + "?lat=" + latitude
                + "&lon=" + longitude
                + "&units=metric"
                + "&appid=" + ApiConfig.OPEN_WEATHER_API_KEY;

        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("Weather request failed with status " + responseCode);
            }

            String responseText;
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                responseText = new String(output.toByteArray(), StandardCharsets.UTF_8);
            }

            JSONObject response = new JSONObject(responseText);
            JSONObject mainObject = response.optJSONObject("main");
            JSONArray weatherArray = response.optJSONArray("weather");

            double temperature = mainObject == null ? Double.NaN : mainObject.optDouble("temp", Double.NaN);
            String condition = "Unknown";
            if (weatherArray != null && weatherArray.length() > 0) {
                JSONObject firstWeather = weatherArray.getJSONObject(0);
                condition = firstWeather.optString("description", firstWeather.optString("main", "Unknown"));
            }

            return new WeatherInfo(temperature, capitalizeWords(condition));
        } finally {
            connection.disconnect();
        }
    }

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
