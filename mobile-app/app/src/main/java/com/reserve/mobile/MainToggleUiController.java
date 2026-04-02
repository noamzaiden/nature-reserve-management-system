package com.reserve.mobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.widget.ImageButton;

import com.google.android.material.button.MaterialButton;

final class MainToggleUiController {

    private static final int TOGGLE_ACTIVE_FILL = Color.parseColor("#E3F0E2");
    private static final int TOGGLE_INACTIVE_FILL = Color.parseColor("#F8FCF7");
    private static final int TOGGLE_ACTIVE_STROKE = Color.parseColor("#5E8B67");
    private static final int TOGGLE_INACTIVE_STROKE = Color.parseColor("#A4BCA8");
    private static final int WEATHER_ICON_ACTIVE = Color.parseColor("#2F6E4A");
    private static final int WEATHER_ICON_INACTIVE = Color.parseColor("#496D54");

    void updateLabels(
            Context context,
            ReserveMapHelper reserveMapHelper,
            MaterialButton poiToggleButton,
            MaterialButton hazardToggleButton,
            ImageButton weatherToggleButton,
            boolean showWeather
    ) {
        boolean showPois = reserveMapHelper.isShowingPois();
        boolean showHazards = reserveMapHelper.isShowingHazards();

        poiToggleButton.setText(showPois ? R.string.poi_on : R.string.poi_off);
        hazardToggleButton.setText(showHazards ? R.string.hazards_on : R.string.hazards_off);

        applyToggleStyle(poiToggleButton, showPois);
        applyToggleStyle(hazardToggleButton, showHazards);
        updateWeatherToggleIcon(context, weatherToggleButton, showWeather);
    }

    private void applyToggleStyle(MaterialButton button, boolean enabled) {
        button.setBackgroundTintList(ColorStateList.valueOf(enabled ? TOGGLE_ACTIVE_FILL : TOGGLE_INACTIVE_FILL));
        button.setStrokeColor(ColorStateList.valueOf(enabled ? TOGGLE_ACTIVE_STROKE : TOGGLE_INACTIVE_STROKE));
    }

    private void updateWeatherToggleIcon(Context context, ImageButton button, boolean showWeather) {
        button.setImageTintList(ColorStateList.valueOf(showWeather ? WEATHER_ICON_ACTIVE : WEATHER_ICON_INACTIVE));
        button.setContentDescription(context.getString(showWeather
                ? R.string.weather_toggle_on
                : R.string.weather_toggle_off));
    }
}

