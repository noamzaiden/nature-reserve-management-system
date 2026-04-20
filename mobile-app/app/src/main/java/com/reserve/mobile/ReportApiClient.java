package com.reserve.mobile;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ReportApiClient {

    private static final String REPORTS_PATH = "/reports";
    private static final String BOUNDARY_PREFIX = "----ReserveTraveler";
    private static final String ATTACHMENTS_FIELD = "attachments";
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final String LINE_BREAK = "\r\n";

    public void submitTravelerReport(ContentResolver contentResolver, TravelerReportData reportData) throws Exception {
        String boundary = createBoundary();
        HttpURLConnection connection = openMultipartPostConnection(boundary);

        try {
            writeReportRequest(contentResolver, reportData, boundary, connection);
            requireSuccessResponse(connection, "Report upload failed with status");
        } finally {
            connection.disconnect();
        }
    }

    private void writeReportRequest(ContentResolver contentResolver, TravelerReportData reportData,
                                    String boundary, HttpURLConnection connection) throws Exception {
        try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
            writeReportFields(output, boundary, reportData);
            writeAttachments(contentResolver, output, boundary, reportData);
            finishMultipartBody(output, boundary);
        }
    }

    private void writeReportFields(DataOutputStream output, String boundary, TravelerReportData reportData) throws Exception {
        writeFormField(output, boundary, "reserveId", String.valueOf(reportData.getReserveId()));
        writeFormField(output, boundary, "type", reportData.getType());
        writeFormField(output, boundary, "reporterName", reportData.getReporterName());
        writeFormField(output, boundary, "description", reportData.getDescription());
        writeFormField(output, boundary, "latitude", String.valueOf(reportData.getLatitude()));
        writeFormField(output, boundary, "longitude", String.valueOf(reportData.getLongitude()));
    }

    private void writeAttachments(ContentResolver contentResolver, DataOutputStream output,
                                  String boundary, TravelerReportData reportData) throws Exception {
        for (Uri attachmentUri : reportData.getAttachmentUris()) {
            writeFileField(contentResolver, output, boundary, attachmentUri);
        }
    }

    private void writeFormField(DataOutputStream output, String boundary, String name, String value) throws Exception {
        output.writeBytes("--" + boundary + LINE_BREAK);
        output.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"" + LINE_BREAK + LINE_BREAK);
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.writeBytes(LINE_BREAK);
    }

    private void writeFileField(ContentResolver contentResolver, DataOutputStream output,
                                String boundary, Uri attachmentUri) throws Exception {
        String mimeType = contentResolver.getType(attachmentUri);
        if (mimeType == null) {
            mimeType = DEFAULT_MIME_TYPE;
        }

        output.writeBytes("--" + boundary + LINE_BREAK);
        output.writeBytes("Content-Disposition: form-data; name=\"" + ATTACHMENTS_FIELD + "\"; filename=\"attachment\"" + LINE_BREAK);
        output.writeBytes("Content-Type: " + mimeType + LINE_BREAK + LINE_BREAK);

        InputStream input = contentResolver.openInputStream(attachmentUri);
        if (input == null) {
            throw new IllegalStateException("Could not open selected file");
        }

        try (BufferedInputStream buffered = new BufferedInputStream(input)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = buffered.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }

        output.writeBytes(LINE_BREAK);
    }

    private void finishMultipartBody(DataOutputStream output, String boundary) throws Exception {
        output.writeBytes("--" + boundary + "--" + LINE_BREAK);
    }

    private String createBoundary() {
        return BOUNDARY_PREFIX + System.currentTimeMillis();
    }

    private HttpURLConnection openMultipartPostConnection(String boundary) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BuildConfig.BACKEND_API_BASE + REPORTS_PATH).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        return connection;
    }

    private void requireSuccessResponse(HttpURLConnection connection, String errorPrefix) throws Exception {
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException(errorPrefix + " " + responseCode);
        }
    }
}
