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

final class MainWeatherController {

    private final Activity activity;
    private final WeatherRepository weatherRepository;
    private final ExecutorService executorService;

    private LatLng lastWeatherLatLng;
    private WeatherInfo currentWeather;
    private List<WeatherHourlyInfo> currentHourly = new ArrayList<>();
    private long lastWeatherLoadedAt;
    private boolean expanded;

    // Creates controller that owns weather UI state and refresh logic.
    MainWeatherController(Activity activity, WeatherRepository weatherRepository, ExecutorService executorService) {
        this.activity = activity;
        this.weatherRepository = weatherRepository;
        this.executorService = executorService;
    }

    // Flips expanded state for hourly weather panel.
    void toggleExpanded() {
        expanded = !expanded;
    }

    // Shows/hides weather overlay and refreshes current + hourly data when needed.
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

        weatherOverlay.setVisibility(View.VISIBLE);
        weatherHourlyPanel.setVisibility(expanded ? View.VISIBLE : View.GONE);
        weatherExpandButton.setImageResource(expanded
                ? android.R.drawable.arrow_up_float
                : android.R.drawable.arrow_down_float);

        if (currentUserLatLng == null) {
            weatherNowText.setText(R.string.weather_waiting_location);
            if (expanded) {
                weatherHourlyText.setText(R.string.weather_hourly_waiting_location);
            }
            return;
        }

        if (!weatherRepository.hasApiKey()) {
            weatherNowText.setText(R.string.weather_missing_key);
            if (expanded) {
                weatherHourlyText.setText(R.string.weather_hourly_unavailable);
            }
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

        weatherNowText.setText(R.string.weather_loading);
        if (expanded) {
            weatherHourlyText.setText(R.string.weather_hourly_loading);
        }

        LatLng requestLocation = currentUserLatLng;
        executorService.execute(() -> {
            try {
                WeatherInfo loadedWeather = needCurrentLoad
                        ? weatherRepository.loadCurrentWeather(requestLocation.latitude, requestLocation.longitude)
                        : currentWeather;
                List<WeatherHourlyInfo> loadedHourly = needHourlyLoad
                        ? weatherRepository.loadHourlyWeather(requestLocation.latitude, requestLocation.longitude, 6)
                        : currentHourly;

                activity.runOnUiThread(() -> {
                    currentWeather = loadedWeather;
                    currentHourly = loadedHourly;
                    lastWeatherLatLng = requestLocation;
                    lastWeatherLoadedAt = System.currentTimeMillis();
                    if (showWeather) {
                        renderCurrentWeather(weatherNowText);
                        renderHourlyWeather(weatherHourlyText);
                    }
                });
            } catch (Exception exception) {
                activity.runOnUiThread(() -> {
                    if (showWeather) {
                        weatherNowText.setText(R.string.weather_unavailable);
                        if (expanded) {
                            weatherHourlyText.setText(R.string.weather_hourly_unavailable);
                        }
                    }
                });
            }
        });
    }

    // Writes current weather line into compact overlay.
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

    // Renders a compact multi-line hourly forecast summary.
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
            WeatherHourlyInfo hourly = currentHourly.get(index);
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

    // Decides whether cache is stale by age or travel distance.
    private boolean shouldReloadWeather(LatLng nowLocation) {
        if (currentWeather == null || lastWeatherLatLng == null) {
            return true;
        }

        long age = System.currentTimeMillis() - lastWeatherLoadedAt;
        if (age > 10 * 60 * 1000) {
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
        return results[0] > 1000;
    }
}

