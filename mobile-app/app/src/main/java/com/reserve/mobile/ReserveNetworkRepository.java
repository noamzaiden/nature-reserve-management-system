package com.reserve.mobile;

import android.content.ContentResolver;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReserveNetworkRepository {

    // Downloads reserves list and maps JSON into Reserve objects.
    public List<Reserve> loadReserves() throws Exception {
        return parseReserves(loadJsonArray("/reserves"));
    }

    // Downloads published events for each reserve and returns combined hazards.
    public List<Event> loadPublishedHazards(List<Reserve> reserves) throws Exception {
        List<Event> hazards = new ArrayList<>();

        for (Reserve reserve : reserves) {
            hazards.addAll(parseHazardsForReserve(reserve));
        }

        return hazards;
    }

    // Maps one reserve JSON object into app model.
    private Reserve parseReserve(JSONObject reserve) throws Exception {
        String name = reserve.optString("name", "Unknown reserve");
        return new Reserve(
                reserve.getLong("id"),
                name,
                reserve.optString("displayName", name),
                reserve.optDouble("centerLatitude", Double.NaN),
                reserve.optDouble("centerLongitude", Double.NaN),
                parseAreaBounds(reserve.optJSONObject("area"))
        );
    }

    // Turns a reserve JSON array into a list of Reserve objects.
    private List<Reserve> parseReserves(JSONArray response) throws Exception {
        List<Reserve> reserves = new ArrayList<>();
        for (int index = 0; index < response.length(); index++) {
            reserves.add(parseReserve(response.getJSONObject(index)));
        }
        return reserves;
    }

    // Loads hazards for one reserve and maps them into PublicEvent objects.
    private List<Event> parseHazardsForReserve(Reserve reserve) throws Exception {
        JSONArray response = loadJsonArray("/events?reserveId=" + reserve.getId());
        List<Event> hazards = new ArrayList<>();
        for (int index = 0; index < response.length(); index++) {
            hazards.add(parsePublicEvent(reserve.getId(), response.getJSONObject(index)));
        }
        return hazards;
    }


    private AreaBounds parseAreaBounds(JSONObject area) {
        if (area == null) {
            return null;
        }
        return new AreaBounds(
                area.optDouble("minLatitude"),
                area.optDouble("maxLatitude"),
                area.optDouble("minLongitude"),
                area.optDouble("maxLongitude")
        );
    }

    // Maps one event JSON object into PublicEvent.
    private Event parsePublicEvent(long reserveId, JSONObject event) {
        return new Event(
                reserveId,
                event.optString("type", "OTHER"),
                event.optString("priority", "LOW"),
                event.optString("description", ""),
                event.optDouble("latitude", Double.NaN),
                event.optDouble("longitude", Double.NaN)
        );
    }

    // Uploads one traveler report as multipart/form-data with optional attachments.
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

    // Writes a plain text multipart field into request body.
    private void writeFormField(DataOutputStream output, String boundary, String name, String value) throws Exception {
        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.writeBytes("\r\n");
    }

    // Writes one selected file as multipart attachment.
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

    // Runs a GET request and returns response JSON text.
    private JSONArray loadJsonArray(String path) throws Exception {
        return new JSONArray(readJsonFromGet(ApiConfig.BACKEND_API_BASE + path));
    }

    // Runs a GET request and returns response JSON text.
    private String readJsonFromGet(String url) throws Exception {
        HttpURLConnection connection = HttpUtils.openJsonGetConnection(url);

        try {
            HttpUtils.requireSuccessResponse(connection, "GET failed with status");
            return HttpUtils.readResponseText(connection);
        } finally {
            connection.disconnect();
        }
    }
}
