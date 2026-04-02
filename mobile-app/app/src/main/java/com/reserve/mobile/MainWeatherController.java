package com.reserve.mobile;

import android.app.Activity;
import android.location.Location;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.concurrent.ExecutorService;

final class MainWeatherController {

    private final Activity activity;
    private final WeatherRepository weatherRepository;
    private final ExecutorService executorService;

    private LatLng lastWeatherLatLng;
    private WeatherInfo currentWeather;
    private long lastWeatherLoadedAt;

    MainWeatherController(Activity activity, WeatherRepository weatherRepository, ExecutorService executorService) {
        this.activity = activity;
        this.weatherRepository = weatherRepository;
        this.executorService = executorService;
    }

    void refreshWeather(boolean showWeather, boolean forceRefresh, LatLng currentUserLatLng, TextView weatherText) {
        if (!showWeather) {
            weatherText.setVisibility(View.GONE);
            return;
        }

        weatherText.setVisibility(View.VISIBLE);

        if (currentUserLatLng == null) {
            weatherText.setText(R.string.weather_waiting_location);
            return;
        }

        if (!weatherRepository.hasApiKey()) {
            weatherText.setText(R.string.weather_missing_key);
            return;
        }

        if (!forceRefresh && !shouldReloadWeather(currentUserLatLng)) {
            if (currentWeather != null) {
                weatherText.setText(activity.getString(
                        R.string.weather_ready,
                        currentWeather.getTemperatureCelsius(),
                        currentWeather.getCondition()
                ));
            }
            return;
        }

        weatherText.setText(R.string.weather_loading);
        LatLng requestLocation = currentUserLatLng;
        executorService.execute(() -> {
            try {
                WeatherInfo loadedWeather = weatherRepository.loadCurrentWeather(
                        requestLocation.latitude,
                        requestLocation.longitude
                );
                activity.runOnUiThread(() -> {
                    currentWeather = loadedWeather;
                    lastWeatherLatLng = requestLocation;
                    lastWeatherLoadedAt = System.currentTimeMillis();
                    if (showWeather) {
                        weatherText.setText(activity.getString(
                                R.string.weather_ready,
                                loadedWeather.getTemperatureCelsius(),
                                loadedWeather.getCondition()
                        ));
                    }
                });
            } catch (Exception exception) {
                activity.runOnUiThread(() -> {
                    if (showWeather) {
                        weatherText.setText(R.string.weather_unavailable);
                    }
                });
            }
        });
    }

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

