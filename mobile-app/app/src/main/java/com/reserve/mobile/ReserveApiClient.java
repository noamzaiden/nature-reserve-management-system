package com.reserve.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ReserveApiClient {

    private static final String RESERVES_PATH = "/reserves";
    private static final String EVENTS_PATH = "/events?reserveId=";
    public List<Reserve> loadReserves() throws Exception {
        return parseReserves(loadJsonArray(RESERVES_PATH));
    }

    public List<Event> loadPublishedHazards(List<Reserve> reserves) throws Exception {
        List<Event> hazards = new ArrayList<>();

        for (Reserve reserve : reserves) {
            hazards.addAll(parseHazardsForReserve(reserve));
        }

        return hazards;
    }

    public List<Poi> loadPois(List<Reserve> reserves) throws Exception {
        List<Poi> pois = new ArrayList<>();

        for (Reserve reserve : reserves) {
            pois.addAll(parsePoisForReserve(reserve));
        }

        return pois;
    }

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

    private List<Reserve> parseReserves(JSONArray response) throws Exception {
        List<Reserve> reserves = new ArrayList<>();
        for (int index = 0; index < response.length(); index++) {
            reserves.add(parseReserve(response.getJSONObject(index)));
        }
        return reserves;
    }

    private List<Event> parseHazardsForReserve(Reserve reserve) throws Exception {
        JSONArray response = loadJsonArray(EVENTS_PATH + reserve.getId());
        List<Event> hazards = new ArrayList<>();
        for (int index = 0; index < response.length(); index++) {
            hazards.add(parseEvent(reserve.getId(), response.getJSONObject(index)));
        }
        return hazards;
    }

    private List<Poi> parsePoisForReserve(Reserve reserve) throws Exception {
        JSONArray response = loadJsonArray(RESERVES_PATH + "/" + reserve.getId() + "/pois");
        List<Poi> pois = new ArrayList<>();
        for (int index = 0; index < response.length(); index++) {
            pois.add(parsePoi(reserve.getId(), response.getJSONObject(index)));
        }
        return pois;
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

    private Poi parsePoi(long reserveId, JSONObject poi) {
        return new Poi(
                poi.optLong("reserveId", reserveId),
                poi.optString("typeName", "POI"),
                poi.optString("name", "POI"),
                poi.optString("description", ""),
                poi.optDouble("latitude", Double.NaN),
                poi.optDouble("longitude", Double.NaN)
        );
    }

    private JSONArray loadJsonArray(String path) throws Exception {
        return new JSONArray(readJsonFromGet(BuildConfig.BACKEND_API_BASE + path));
    }

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
