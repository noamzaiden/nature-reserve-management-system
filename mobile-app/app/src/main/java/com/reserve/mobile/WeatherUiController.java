package com.reserve.mobile;

import android.app.Activity;
import android.location.Location;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

final class WeatherUiController {

    private static final long WEATHER_CACHE_MAX_AGE_MS = 30L * 60L * 1000L;
    private static final float WEATHER_RELOAD_DISTANCE_METERS = 1000f;
    private static final int HOURLY_FORECAST_HOURS = 6;

    private final Activity activity;
    private final WeatherApiClient weatherApiClient;
    private final ExecutorService executorService;

    private LatLng lastWeatherLatLng;
    private WeatherCurrent currentWeather;
    private List<WeatherHourlyForecast> currentHourly = new ArrayList<>();
    private long lastWeatherLoadedAt;
    private boolean expanded;

    WeatherUiController(Activity activity, WeatherApiClient weatherApiClient, ExecutorService executorService) {
        this.activity = activity;
        this.weatherApiClient = weatherApiClient;
        this.executorService = executorService;
    }

    void toggleExpanded() {
        expanded = !expanded;
    }

    void refreshWeather(
            boolean showWeather,
            boolean forceRefresh,
            LatLng currentUserLatLng,
            View weatherOverlay,
            TextView weatherNowText,
            View weatherHourlyPanel,
            TextView weatherHourlyText,
            ImageButton weatherExpandButton
    ) {
        if (!showWeather) {
            weatherOverlay.setVisibility(View.GONE);
            return;
        }

        showWeatherPanel(weatherOverlay, weatherHourlyPanel, weatherExpandButton);

        if (currentUserLatLng == null) {
            showStatus(
                    weatherNowText,
                    weatherHourlyText,
                    R.string.weather_waiting_location,
                    R.string.weather_hourly_waiting_location
            );
            return;
        }

        if (!weatherApiClient.hasApiKey()) {
            showStatus(
                    weatherNowText,
                    weatherHourlyText,
                    R.string.weather_missing_key,
                    R.string.weather_hourly_unavailable
            );
            return;
        }

        boolean shouldReload = shouldReloadWeather(currentUserLatLng);
        boolean needCurrentLoad = currentWeather == null || forceRefresh || shouldReload;
        boolean needHourlyLoad = expanded && (currentHourly.isEmpty() || forceRefresh || shouldReload);

        if (!needCurrentLoad && !needHourlyLoad) {
            renderCurrentWeather(weatherNowText);
            renderHourlyWeather(weatherHourlyText);
            return;
        }

        showStatus(
                weatherNowText,
                weatherHourlyText,
                R.string.weather_loading,
                R.string.weather_hourly_loading
        );

        executorService.execute(() -> {
            try {
                WeatherCurrent loadedWeather = needCurrentLoad
                        ? weatherApiClient.loadCurrentWeather(currentUserLatLng.latitude, currentUserLatLng.longitude)
                        : currentWeather;
                List<WeatherHourlyForecast> loadedHourly = needHourlyLoad
                        ? weatherApiClient.loadHourlyWeather(
                                currentUserLatLng.latitude,
                                currentUserLatLng.longitude,
                                HOURLY_FORECAST_HOURS
                        )
                        : currentHourly;

                activity.runOnUiThread(() -> {
                    currentWeather = loadedWeather;
                    currentHourly = loadedHourly;
                    lastWeatherLatLng = currentUserLatLng;
                    lastWeatherLoadedAt = System.currentTimeMillis();
                    renderCurrentWeather(weatherNowText);
                    renderHourlyWeather(weatherHourlyText);
                });
            } catch (Exception exception) {
                activity.runOnUiThread(() -> {
                    showStatus(
                            weatherNowText,
                            weatherHourlyText,
                            R.string.weather_unavailable,
                            R.string.weather_hourly_unavailable
                    );
                });
            }
        });
    }

    private void showWeatherPanel(View weatherOverlay, View weatherHourlyPanel, ImageButton weatherExpandButton) {
        weatherOverlay.setVisibility(View.VISIBLE);
        weatherHourlyPanel.setVisibility(expanded ? View.VISIBLE : View.GONE);
        weatherExpandButton.setImageResource(expanded
                ? android.R.drawable.arrow_up_float
                : android.R.drawable.arrow_down_float);
    }

    private void showStatus(TextView weatherNowText, TextView weatherHourlyText, int nowTextResId, int hourlyTextResId) {
        weatherNowText.setText(nowTextResId);
        if (expanded) {
            weatherHourlyText.setText(hourlyTextResId);
        }
    }

    private void renderCurrentWeather(TextView weatherNowText) {
        if (currentWeather == null) {
            weatherNowText.setText(R.string.weather_unavailable);
            return;
        }
        weatherNowText.setText(activity.getString(
                R.string.weather_ready,
                currentWeather.getTemperatureCelsius(),
                currentWeather.getCondition()
        ));
    }

    private void renderHourlyWeather(TextView weatherHourlyText) {
        if (!expanded) {
            return;
        }
        if (currentHourly == null || currentHourly.isEmpty()) {
            weatherHourlyText.setText(R.string.weather_hourly_unavailable);
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < currentHourly.size(); index++) {
            WeatherHourlyForecast hourly = currentHourly.get(index);
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(String.format(
                    Locale.US,
                    "%s  %.1fC  %s",
                    hourly.getHourLabel(),
                    hourly.getTemperatureCelsius(),
                    hourly.getCondition()
            ));
        }
        weatherHourlyText.setText(builder.toString());
    }

    private boolean shouldReloadWeather(LatLng nowLocation) {
        if (currentWeather == null || lastWeatherLatLng == null) {
            return true;
        }

        if (System.currentTimeMillis() - lastWeatherLoadedAt > WEATHER_CACHE_MAX_AGE_MS) {
            return true;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                lastWeatherLatLng.latitude,
                lastWeatherLatLng.longitude,
                nowLocation.latitude,
                nowLocation.longitude,
                results
        );
        return results[0] > WEATHER_RELOAD_DISTANCE_METERS;
    }
}

