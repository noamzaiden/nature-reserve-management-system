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

    void setReportPanelVisible(boolean visible, View reportPanel, View bottomSpacer, MaterialButton reportToggleButton) {
        reportPanelVisible = visible;
        reportPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        bottomSpacer.setVisibility(visible ? View.GONE : View.VISIBLE);
        reportToggleButton.setText(visible ? R.string.hide_report_button : R.string.report_event_button);
    }

    boolean toggleReportPanel(View reportPanel, View bottomSpacer, MaterialButton reportToggleButton) {
        setReportPanelVisible(!reportPanelVisible, reportPanel, bottomSpacer, reportToggleButton);
        return reportPanelVisible;
    }

    boolean isReportPanelVisible() {
        return reportPanelVisible;
    }

    void startManualLocationSelection(MaterialButton manualLocationButton) {
        selectingManualLocation = true;
        manualLocationButton.setText(R.string.report_manual_location_waiting);
    }

    boolean isSelectingManualLocation() {
        return selectingManualLocation;
    }

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
            ensureManualMarker(latLng, googleMap, context);
        }

        updateReportLocationText(reportLocationText, currentUserLatLng, context);
    }

    LatLng getManualReportLatLng() {
        return manualReportLatLng;
    }

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

    void restoreManualLocationMarker(GoogleMap googleMap, Context context) {
        if (manualReportLatLng == null || googleMap == null) {
            return;
        }

        manualReportMarker = null;
        ensureManualMarker(manualReportLatLng, googleMap, context);
    }

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

    private void ensureManualMarker(LatLng latLng, GoogleMap googleMap, Context context) {
        if (manualReportMarker == null) {
            manualReportMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(context.getString(R.string.report_manual_marker_title)));
            return;
        }

        manualReportMarker.setPosition(latLng);
    }
}

