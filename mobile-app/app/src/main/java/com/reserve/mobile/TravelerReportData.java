package com.reserve.mobile;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public final class TravelerReportData {

    private final long reserveId;
    private final String type;
    private final String reporterName;
    private final String description;
    private final double latitude;
    private final double longitude;
    private final List<Uri> attachmentUris;

    public TravelerReportData(long reserveId, String type, String reporterName, String description,
                              double latitude, double longitude, List<Uri> attachmentUris) {
        this.reserveId = reserveId;
        this.type = type;
        this.reporterName = reporterName;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.attachmentUris = attachmentUris == null ? new ArrayList<>() : new ArrayList<>(attachmentUris);
    }

    public long getReserveId() {
        return reserveId;
    }

    public String getType() {
        return type;
    }

    public String getReporterName() {
        return reporterName;
    }

    public String getDescription() {
        return description;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public List<Uri> getAttachmentUris() {
        return attachmentUris;
    }
}
