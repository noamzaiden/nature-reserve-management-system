package com.reserve.mobile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

final class ReportSubmissionController {

    interface Host {
        void setBusyState(boolean busy, String message);

        void requestLocationTracking();

        void onReportLocationResolved(LatLng latLng);

        void onMissingLocationForReport();

        void onReportSubmitted();

        void reloadHazards();

        void updateServerStatus(boolean online);
    }

    private final Activity activity;
    private final ContentResolver contentResolver;
    private final FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executorService;
    private final ReserveService reserveService;

    ReportSubmissionController(Activity activity,
                               ContentResolver contentResolver,
                               FusedLocationProviderClient fusedLocationClient,
                               ExecutorService executorService,
                               ReserveService reserveService) {
        this.activity = activity;
        this.contentResolver = contentResolver;
        this.fusedLocationClient = fusedLocationClient;
        this.executorService = executorService;
        this.reserveService = reserveService;
    }

    // Validates report data, resolves a location, and uploads the report.
    @SuppressLint("MissingPermission")
    void submitReport(Reserve selectedReserve,
                      String reportType,
                      String reporterName,
                      String description,
                      LatLng manualLocation,
                      List<Uri> selectedMediaUris,
                      boolean hasLocationPermission,
                      Host host) {
        String trimmedDescription = description == null ? "" : description.trim();
        if (selectedReserve == null) {
            showShortToast(R.string.report_requires_reserve);
            return;
        }
        if (trimmedDescription.isEmpty()) {
            showShortToast(R.string.report_requires_description);
            return;
        }
        if (manualLocation != null) {
            host.onReportLocationResolved(manualLocation);
            uploadReport(
                    selectedReserve,
                    safeReportType(reportType),
                    reporterName,
                    trimmedDescription,
                    manualLocation,
                    selectedMediaUris,
                    host
            );
            return;
        }
        if (!hasLocationPermission) {
            showShortToast(R.string.report_requires_location);
            host.requestLocationTracking();
            return;
        }

        host.setBusyState(true, activity.getString(R.string.status_fetching_phone_location));
        // Request one fresh GPS fix so the uploaded report uses the latest coordinates.
        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        new CancellationTokenSource().getToken()
                )
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        handleMissingLocation(host);
                        return;
                    }
                    LatLng resolvedLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    host.onReportLocationResolved(resolvedLatLng);
                    uploadReport(
                            selectedReserve,
                            safeReportType(reportType),
                            reporterName,
                            trimmedDescription,
                            resolvedLatLng,
                            selectedMediaUris,
                            host
                    );
                })
                .addOnFailureListener(exception -> handleMissingLocation(host));
    }

    // Uploads the validated report payload on the shared background executor.
    private void uploadReport(Reserve selectedReserve,
                              String reportType,
                              String reporterName,
                              String description,
                              LatLng reportLatLng,
                              List<Uri> selectedMediaUris,
                              Host host) {
        host.setBusyState(true, activity.getString(R.string.status_report_sending));
        List<Uri> attachmentSnapshot = new ArrayList<>(selectedMediaUris);
        executorService.execute(() -> {
            try {
                TravelerReportData reportData = new TravelerReportData(
                        selectedReserve.getId(),
                        reportType,
                        reporterName == null ? "" : reporterName.trim(),
                        description,
                        reportLatLng.latitude,
                        reportLatLng.longitude,
                        attachmentSnapshot
                );
                reserveService.submitTravelerReport(contentResolver, reportData);
                activity.runOnUiThread(() -> {
                    host.updateServerStatus(true);
                    host.onReportSubmitted();
                    host.setBusyState(false, activity.getString(R.string.status_report_sent));
                    showLongToast(R.string.report_sent_toast);
                    host.reloadHazards();
                });
            } catch (Exception exception) {
                activity.runOnUiThread(() -> {
                    host.updateServerStatus(false);
                    host.setBusyState(false, activity.getString(R.string.status_report_failed));
                });
            }
        });
    }

    // Restores UI state when a fresh location fix could not be obtained.
    private void handleMissingLocation(Host host) {
        host.setBusyState(false, activity.getString(R.string.status_location_waiting));
        showShortToast(R.string.report_requires_location);
        host.onMissingLocationForReport();
    }

    private String safeReportType(String reportType) {
        if (reportType == null) {
            return "OTHER";
        }
        String trimmedType = reportType.trim();
        return trimmedType.isEmpty() ? "OTHER" : trimmedType;
    }

    private void showShortToast(int messageResId) {
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show();
    }

    private void showLongToast(int messageResId) {
        Toast.makeText(activity, messageResId, Toast.LENGTH_LONG).show();
    }
}
