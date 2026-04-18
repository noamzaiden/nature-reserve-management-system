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

public class ReserveService {

    private static final String RESERVES_PATH = "/reserves";
    private static final String EVENTS_PATH = "/events?reserveId=";
    private static final String REPORTS_PATH = "/reports";

    // Downloads reserves list and maps JSON into Reserve objects.
    public List<Reserve> loadReserves() throws Exception {
        return parseReserves(loadJsonArray(RESERVES_PATH));
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

    // Loads hazards for one reserve and maps them into Event objects.
    private List<Event> parseHazardsForReserve(Reserve reserve) throws Exception {
        JSONArray response = loadJsonArray(EVENTS_PATH + reserve.getId());
        List<Event> hazards = new ArrayList<>();
        for (int index = 0; index < response.length(); index++) {
            hazards.add(parseEvent(reserve.getId(), response.getJSONObject(index)));
        }
        return hazards;
    }

    // Maps area JSON into a simple rectangular boundary.
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

    // Maps one event JSON object into the app event model.
    private Event parseEvent(long reserveId, JSONObject event) {
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
        HttpURLConnection connection = openMultipartPostConnection(REPORTS_PATH, boundary);

        try {
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

            requireSuccessResponse(connection, "Report upload failed with status");
        } finally {
            connection.disconnect();
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
        return new JSONArray(readJsonFromGet(BuildConfig.BACKEND_API_BASE + path));
    }

    // Runs a GET request and returns response JSON text.
    private String readJsonFromGet(String url) throws Exception {
        HttpURLConnection connection = openJsonGetConnection(url);

        try {
            requireSuccessResponse(connection, "GET failed with status");
            return readResponseText(connection);
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openJsonGetConnection(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    private HttpURLConnection openMultipartPostConnection(String path, String boundary) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BuildConfig.BACKEND_API_BASE + path).openConnection();
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

    private String readResponseText(HttpURLConnection connection) throws Exception {
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
