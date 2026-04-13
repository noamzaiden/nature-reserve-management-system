package com.reserve.mobile;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class HttpUtils {

    private HttpUtils() {
    }

    // Opens a basic JSON GET connection with a common Accept header.
    static HttpURLConnection openJsonGetConnection(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    // Throws when HTTP status is outside successful 2xx range.
    static void requireSuccessResponse(HttpURLConnection connection, String errorPrefix) throws Exception {
        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException(errorPrefix + " " + responseCode);
        }
    }

    // Reads full response body text as UTF-8.
    static String readResponseText(HttpURLConnection connection) throws Exception {
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

