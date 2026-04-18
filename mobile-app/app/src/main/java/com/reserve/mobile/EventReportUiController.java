package com.reserve.mobile;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

final class EventReportUiController {

    private boolean reportPanelVisible;
    private boolean selectingManualLocation;
    private LatLng manualReportLatLng;
    private Marker manualReportMarker;

    // Applies current panel visibility and keeps related controls in sync.
    void setReportPanelVisible(boolean visible, View reportPanel, View bottomSpacer, MaterialButton reportToggleButton) {
        reportPanelVisible = visible;
        reportPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        bottomSpacer.setVisibility(visible ? View.GONE : View.VISIBLE);
        reportToggleButton.setText(visible ? R.string.hide_report_button : R.string.report_event_button);
    }

    // Toggles panel visibility and returns the new visible state.
    boolean toggleReportPanel(View reportPanel, View bottomSpacer, MaterialButton reportToggleButton) {
        setReportPanelVisible(!reportPanelVisible, reportPanel, bottomSpacer, reportToggleButton);
        return reportPanelVisible;
    }

    // Puts report flow into map-tap mode for manual location pick.
    void startManualLocationSelection(MaterialButton manualLocationButton) {
        selectingManualLocation = true;
        manualLocationButton.setText(R.string.report_manual_location_waiting);
    }

    // Returns true when the next map tap should set report location.
    boolean isSelectingManualLocation() {
        return selectingManualLocation;
    }

    // Stores tapped location, updates marker, and refreshes location label.
    void saveManualLocation(LatLng latLng,
                            GoogleMap googleMap,
                            MaterialButton manualLocationButton,
                            TextView reportLocationText,
                            LatLng currentUserLatLng,
                            Context context) {
        selectingManualLocation = false;
        manualReportLatLng = latLng;
        manualLocationButton.setText(R.string.report_pick_map_location);

        if (googleMap != null) {
            if (manualReportMarker == null) {
                manualReportMarker = googleMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(context.getString(R.string.report_manual_marker_title)));
            } else {
                manualReportMarker.setPosition(latLng);
            }
        }

        updateReportLocationText(reportLocationText, currentUserLatLng, context);
    }

    // Returns manual location chosen by user, or null when not set.
    LatLng getManualReportLatLng() {
        return manualReportLatLng;
    }

    // Clears manual marker/location and restores default report button label.
    void clearManualLocation(MaterialButton manualLocationButton,
                             TextView reportLocationText,
                             LatLng currentUserLatLng,
                             Context context) {
        selectingManualLocation = false;
        manualReportLatLng = null;
        if (manualReportMarker != null) {
            manualReportMarker.remove();
            manualReportMarker = null;
        }
        manualLocationButton.setText(R.string.report_pick_map_location);
        updateReportLocationText(reportLocationText, currentUserLatLng, context);
    }

    // Updates report coordinates label using manual location first, then GPS.
    void updateReportLocationText(TextView reportLocationText, LatLng currentUserLatLng, Context context) {
        if (manualReportLatLng != null) {
            reportLocationText.setText(context.getString(
                    R.string.report_location_ready,
                    manualReportLatLng.latitude,
                    manualReportLatLng.longitude
            ));
            return;
        }
        if (currentUserLatLng == null) {
            reportLocationText.setText(R.string.report_location_waiting);
            return;
        }
        reportLocationText.setText(context.getString(
                R.string.report_location_ready,
                currentUserLatLng.latitude,
                currentUserLatLng.longitude
        ));
    }
}

