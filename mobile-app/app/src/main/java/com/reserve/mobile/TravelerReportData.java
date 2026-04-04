package com.reserve.mobile;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class TravelerReportData {

    private final long reserveId;
    private final String type;
    private final String reporterName;
    private final String description;
    private final double latitude;
    private final double longitude;
    private final List<Uri> attachmentUris;

    // Holds one report payload before multipart upload.
    public TravelerReportData(long reserveId, String type, String reporterName, String description,
                              double latitude, double longitude, List<Uri> attachmentUris) {
        this.reserveId = reserveId;
        this.type = type;
        this.reporterName = reporterName;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.attachmentUris = new ArrayList<>(attachmentUris);
    }

    // Returns selected reserve id.
    public long getReserveId() {
        return reserveId;
    }

    // Returns selected report type.
    public String getType() {
        return type;
    }

    // Returns reporter name (optional).
    public String getReporterName() {
        return reporterName;
    }

    // Returns report description text.
    public String getDescription() {
        return description;
    }

    // Returns latitude captured from phone location.
    public double getLatitude() {
        return latitude;
    }

    // Returns longitude captured from phone location.
    public double getLongitude() {
        return longitude;
    }

    // Returns chosen media files to upload.
    public List<Uri> getAttachmentUris() {
        return attachmentUris;
    }
}
