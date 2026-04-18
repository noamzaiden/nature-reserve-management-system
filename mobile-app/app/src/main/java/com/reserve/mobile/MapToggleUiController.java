package com.reserve.mobile;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.ImageButton;

import com.google.android.material.button.MaterialButton;

final class MapToggleUiController {

    private static final int TOGGLE_ACTIVE_FILL = Color.parseColor("#E3F0E2");
    private static final int TOGGLE_INACTIVE_FILL = Color.parseColor("#F8FCF7");
    private static final int TOGGLE_ACTIVE_STROKE = Color.parseColor("#5E8B67");
    private static final int TOGGLE_INACTIVE_STROKE = Color.parseColor("#A4BCA8");
    private static final int WEATHER_ICON_ACTIVE = Color.parseColor("#2F6E4A");
    private static final int WEATHER_ICON_INACTIVE = Color.parseColor("#496D54");

    // Updates labels and styles for POI, hazard, and weather toggles.
    void updateLabels(
            MapController mapController,
            MaterialButton poiToggleButton,
            MaterialButton hazardToggleButton,
            ImageButton weatherToggleButton,
            boolean showWeather
    ) {
        boolean showPois = mapController.isShowingPois();
        boolean showHazards = mapController.isShowingHazards();

        poiToggleButton.setText(showPois ? R.string.poi_on : R.string.poi_off);
        hazardToggleButton.setText(showHazards ? R.string.hazards_on : R.string.hazards_off);

        applyToggleStyle(poiToggleButton, showPois);
        applyToggleStyle(hazardToggleButton, showHazards);
        updateWeatherToggleIcon(weatherToggleButton, showWeather);
    }

    // Applies active/inactive colors to side menu toggle buttons.
    private void applyToggleStyle(MaterialButton button, boolean enabled) {
        button.setBackgroundTintList(ColorStateList.valueOf(enabled ? TOGGLE_ACTIVE_FILL : TOGGLE_INACTIVE_FILL));
        button.setStrokeColor(ColorStateList.valueOf(enabled ? TOGGLE_ACTIVE_STROKE : TOGGLE_INACTIVE_STROKE));
    }

    // Updates weather map button icon style and accessibility text.
    private void updateWeatherToggleIcon(ImageButton button, boolean showWeather) {
        button.setBackgroundResource(showWeather
                ? R.drawable.bg_map_round_button_active
                : R.drawable.bg_map_round_button_inactive);
        button.setImageTintList(ColorStateList.valueOf(showWeather ? WEATHER_ICON_ACTIVE : WEATHER_ICON_INACTIVE));
        button.setAlpha(showWeather ? 1f : 0.8f);
        button.setContentDescription(button.getContext().getString(showWeather
                ? R.string.weather_toggle_on
                : R.string.weather_toggle_off));
    }
}

