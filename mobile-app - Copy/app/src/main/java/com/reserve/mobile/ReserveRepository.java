package com.reserve.mobile;

import android.content.ContentResolver;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReserveRepository {

    public List<ReserveOption> loadReserves() throws Exception {
        JSONArray response = new JSONArray(readJsonFromGet(ApiConfig.BACKEND_API_BASE + "/reserves"));
        List<ReserveOption> reserves = new ArrayList<>();

        for (int index = 0; index < response.length(); index++) {
            JSONObject reserve = response.getJSONObject(index);
            JSONObject area = reserve.optJSONObject("area");

            AreaBounds areaBounds = null;
            if (area != null) {
                areaBounds = new AreaBounds(
                        area.optDouble("minLatitude"),
                        area.optDouble("maxLatitude"),
                        area.optDouble("minLongitude"),
                        area.optDouble("maxLongitude")
                );
            }

            reserves.add(new ReserveOption(
                    reserve.getLong("id"),
                    reserve.optString("name", "Unknown reserve"),
                    reserve.optString("displayName", reserve.optString("name", "Unknown reserve")),
                    reserve.optDouble("centerLatitude", Double.NaN),
                    reserve.optDouble("centerLongitude", Double.NaN),
                    areaBounds
            ));
        }

        return reserves;
    }

    public List<PublicEvent> loadPublishedHazards(List<ReserveOption> reserves) throws Exception {
        List<PublicEvent> hazards = new ArrayList<>();

        for (ReserveOption reserve : reserves) {
            JSONArray response = new JSONArray(readJsonFromGet(ApiConfig.BACKEND_API_BASE + "/events?reserveId=" + reserve.getId()));
            for (int index = 0; index < response.length(); index++) {
                JSONObject event = response.getJSONObject(index);
                hazards.add(new PublicEvent(
                        reserve.getId(),
                        event.optString("type", "OTHER"),
                        event.optString("priority", "LOW"),
                        event.optString("description", ""),
                        event.optDouble("latitude", Double.NaN),
                        event.optDouble("longitude", Double.NaN)
                ));
            }
        }

        return hazards;
    }

    public void submitTravelerReport(ContentResolver resolver, TravelerReportData reportData) throws Exception {
        String boundary = "----ReserveTraveler" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(ApiConfig.BACKEND_API_BASE + "/reports").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (DataOutputStream output = new DataOutputStream(connection.getOutputStream())) {
            writeFormField(output, boundary, "reserveId", String.valueOf(reportData.getReserveId()));
            writeFormField(output, boundary, "type", reportData.getType());
            writeFormField(output, boundary, "reporterName", reportData.getReporterName());
            writeFormField(output, boundary, "description", reportData.getDescription());
            writeFormField(output, boundary, "latitude", String.valueOf(reportData.getLatitude()));
            writeFormField(output, boundary, "longitude", String.valueOf(reportData.getLongitude()));

            for (Uri uri : reportData.getAttachmentUris()) {
                writeFileField(resolver, output, boundary, uri);
            }

            output.writeBytes("--" + boundary + "--\r\n");
            output.flush();
        }

        int responseCode = connection.getResponseCode();
        connection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("Report upload failed with status " + responseCode);
        }
    }

    private void writeFormField(DataOutputStream output, String boundary, String name, String value) throws Exception {
        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.writeBytes("\r\n");
    }

    private void writeFileField(ContentResolver resolver, DataOutputStream output,
                                String boundary, Uri uri) throws Exception {
        String mimeType = resolver.getType(uri);

        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"attachments\"; filename=\"attachment\"\r\n");
        output.writeBytes("Content-Type: " + (mimeType == null ? "application/octet-stream" : mimeType) + "\r\n\r\n");

        InputStream input = resolver.openInputStream(uri);
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

        output.writeBytes("\r\n");
    }

    private String readJsonFromGet(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IllegalStateException("GET failed with status " + responseCode);
            }

            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                return new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }
}
