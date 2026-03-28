package com.reserve.mobile;

import android.content.ContentResolver;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String API_BASE = "http://10.0.2.2:8080/api/public";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<ReserveOption> reserves = new ArrayList<>();
    private final List<Uri> selectedMediaUris = new ArrayList<>();

    private Spinner reserveSpinner;
    private Spinner reportTypeSpinner;
    private TextView statusText;
    private TextView eventCountText;
    private TextView selectedMediaText;
    private LinearLayout eventsContainer;
    private EditText reporterNameInput;
    private EditText reportDescriptionInput;
    private Button refreshButton;
    private Button attachMediaButton;
    private Button submitReportButton;

    private ActivityResultLauncher<String[]> mediaPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        configureTypeSpinner();
        configureMediaPicker();
        configureButtons();
        loadReserves();
    }

    private void bindViews() {
        reserveSpinner = findViewById(R.id.reserve_spinner);
        reportTypeSpinner = findViewById(R.id.report_type_spinner);
        statusText = findViewById(R.id.status_text);
        eventCountText = findViewById(R.id.event_count_text);
        selectedMediaText = findViewById(R.id.selected_media_text);
        eventsContainer = findViewById(R.id.events_container);
        reporterNameInput = findViewById(R.id.reporter_name_input);
        reportDescriptionInput = findViewById(R.id.report_description_input);
        refreshButton = findViewById(R.id.refresh_button);
        attachMediaButton = findViewById(R.id.attach_media_button);
        submitReportButton = findViewById(R.id.submit_report_button);
    }

    private void configureTypeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"FIRE", "BLOCKAGE", "OTHER"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reportTypeSpinner.setAdapter(adapter);
    }

    private void configureMediaPicker() {
        mediaPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenMultipleDocuments(),
                uris -> {
                    selectedMediaUris.clear();
                    if (uris != null) {
                        selectedMediaUris.addAll(uris);
                    }
                    updateSelectedMediaText();
                }
        );
    }

    private void configureButtons() {
        refreshButton.setOnClickListener(view -> loadPublishedEventsForSelectedReserve());
        attachMediaButton.setOnClickListener(view -> mediaPickerLauncher.launch(new String[]{"image/*", "video/*"}));
        submitReportButton.setOnClickListener(view -> submitTravelerReport());
    }

    private void loadReserves() {
        setBusyState(true, "Loading reserves...");
        executorService.execute(() -> {
            try {
                JSONArray response = new JSONArray(readJsonFromGet(API_BASE + "/reserves"));
                List<ReserveOption> loadedReserves = new ArrayList<>();
                for (int index = 0; index < response.length(); index++) {
                    JSONObject reserve = response.getJSONObject(index);
                    loadedReserves.add(new ReserveOption(
                            reserve.getLong("id"),
                            reserve.getString("name"),
                            reserve.optString("region", "")
                    ));
                }

                mainHandler.post(() -> {
                    reserves.clear();
                    reserves.addAll(loadedReserves);
                    ArrayAdapter<ReserveOption> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            reserves
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    reserveSpinner.setAdapter(adapter);
                    reserveSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> loadPublishedEvents(reserves.get(position).id)));
                    setBusyState(false, reserves.isEmpty() ? "No reserves available yet." : "Choose a reserve to see traveler updates.");
                    if (!reserves.isEmpty()) {
                        loadPublishedEvents(reserves.get(0).id);
                    }
                });
            } catch (Exception exception) {
                mainHandler.post(() -> setBusyState(false, "Failed to load reserves. Make sure the backend is running."));
            }
        });
    }

    private void loadPublishedEventsForSelectedReserve() {
        ReserveOption selectedReserve = getSelectedReserve();
        if (selectedReserve == null) {
            Toast.makeText(this, "Choose a reserve first.", Toast.LENGTH_SHORT).show();
            return;
        }
        loadPublishedEvents(selectedReserve.id);
    }

    private void loadPublishedEvents(long reserveId) {
        setBusyState(true, "Loading traveler updates...");
        executorService.execute(() -> {
            try {
                JSONArray response = new JSONArray(readJsonFromGet(API_BASE + "/events?reserveId=" + reserveId));
                List<PublicEvent> events = new ArrayList<>();
                for (int index = 0; index < response.length(); index++) {
                    JSONObject event = response.getJSONObject(index);
                    events.add(new PublicEvent(
                            event.getString("type"),
                            event.getString("priority"),
                            event.optString("description", "No description provided."),
                            event.optString("createdAt", ""),
                            event.optJSONArray("media") == null ? 0 : event.optJSONArray("media").length()
                    ));
                }

                mainHandler.post(() -> {
                    renderEvents(events);
                    setBusyState(false, "Traveler feed updated.");
                });
            } catch (Exception exception) {
                mainHandler.post(() -> setBusyState(false, "Failed to load traveler updates."));
            }
        });
    }

    private void renderEvents(List<PublicEvent> events) {
        eventsContainer.removeAllViews();
        if (events.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText(R.string.empty_events);
            emptyView.setTextColor(0xFF6A675F);
            eventsContainer.addView(emptyView);
            eventCountText.setText(getString(R.string.empty_events));
            return;
        }

        eventCountText.setText(events.size() + " traveler updates are live for this reserve.");
        LayoutInflater inflater = LayoutInflater.from(this);

        for (PublicEvent event : events) {
            View card = inflater.inflate(R.layout.item_event_card, eventsContainer, false);
            TextView badge = card.findViewById(R.id.event_priority_badge);
            TextView type = card.findViewById(R.id.event_type_text);
            TextView description = card.findViewById(R.id.event_description_text);
            TextView meta = card.findViewById(R.id.event_meta_text);

            badge.setText(event.priority);
            badge.setBackground(priorityDrawable(event.priority));
            type.setText(event.type);
            description.setText(event.description);

            StringBuilder metaText = new StringBuilder();
            metaText.append("Published update");
            if (!event.createdAt.isEmpty()) {
                metaText.append(" • ").append(event.createdAt.replace('T', ' '));
            }
            if (event.mediaCount > 0) {
                metaText.append(" • ").append(event.mediaCount).append(" photo/video attachment(s)");
            }
            meta.setText(metaText.toString());

            eventsContainer.addView(card);
        }
    }

    private GradientDrawable priorityDrawable(String priority) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(999f);
        if ("HIGH".equals(priority)) {
            drawable.setColor(0xFFC0392B);
        } else if ("MEDIUM".equals(priority)) {
            drawable.setColor(0xFFD68910);
        } else {
            drawable.setColor(0xFF2471A3);
        }
        return drawable;
    }

    private void submitTravelerReport() {
        ReserveOption selectedReserve = getSelectedReserve();
        if (selectedReserve == null) {
            Toast.makeText(this, "Choose a reserve first.", Toast.LENGTH_SHORT).show();
            return;
        }
        String description = reportDescriptionInput.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(this, "Describe what you saw first.", Toast.LENGTH_SHORT).show();
            return;
        }

        setBusyState(true, "Sending your report...");
        executorService.execute(() -> {
            try {
                postMultipartReport(selectedReserve.id);
                mainHandler.post(() -> {
                    reporterNameInput.setText("");
                    reportDescriptionInput.setText("");
                    selectedMediaUris.clear();
                    updateSelectedMediaText();
                    Toast.makeText(this, "Report sent to the reserve manager for review.", Toast.LENGTH_LONG).show();
                    setBusyState(false, "Traveler report submitted.");
                    loadPublishedEvents(selectedReserve.id);
                });
            } catch (Exception exception) {
                mainHandler.post(() -> setBusyState(false, "Failed to send the report."));
            }
        });
    }

    private void postMultipartReport(long reserveId) throws Exception {
        String boundary = "----ReserveTraveler" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(API_BASE + "/reports").openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
            writeFormField(outputStream, boundary, "reserveId", String.valueOf(reserveId));
            writeFormField(outputStream, boundary, "type", reportTypeSpinner.getSelectedItem().toString());
            writeFormField(outputStream, boundary, "reporterName", reporterNameInput.getText().toString().trim());
            writeFormField(outputStream, boundary, "description", reportDescriptionInput.getText().toString().trim());

            for (Uri uri : selectedMediaUris) {
                writeFileField(outputStream, boundary, uri);
            }

            outputStream.writeBytes("--" + boundary + "--\r\n");
            outputStream.flush();
        }

        int responseCode = connection.getResponseCode();
        if (responseCode < 200 || responseCode >= 300) {
            throw new IllegalStateException("Report upload failed with status " + responseCode);
        }
        connection.disconnect();
    }

    private void writeFormField(DataOutputStream outputStream, String boundary, String name, String value) throws Exception {
        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        outputStream.writeBytes("\r\n");
    }

    private void writeFileField(DataOutputStream outputStream, String boundary, Uri uri) throws Exception {
        ContentResolver resolver = getContentResolver();
        String mimeType = resolver.getType(uri);
        String fileName = "attachment";

        outputStream.writeBytes("--" + boundary + "\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"attachments\"; filename=\"" + fileName + "\"\r\n");
        outputStream.writeBytes("Content-Type: " + (mimeType == null ? "application/octet-stream" : mimeType) + "\r\n\r\n");

        try (InputStream inputStream = resolver.openInputStream(uri);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        outputStream.writeBytes("\r\n");
    }

    private String readJsonFromGet(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toString(StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    private void setBusyState(boolean busy, String message) {
        statusText.setText(message);
        refreshButton.setEnabled(!busy);
        attachMediaButton.setEnabled(!busy);
        submitReportButton.setEnabled(!busy);
        reserveSpinner.setEnabled(!busy);
    }

    private ReserveOption getSelectedReserve() {
        int position = reserveSpinner.getSelectedItemPosition();
        if (position < 0 || position >= reserves.size()) {
            return null;
        }
        return reserves.get(position);
    }

    private void updateSelectedMediaText() {
        if (selectedMediaUris.isEmpty()) {
            selectedMediaText.setText(R.string.selected_media_none);
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(selectedMediaUris.size()).append(" attachment(s) selected");
        selectedMediaText.setText(builder.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdownNow();
    }

    private static class ReserveOption {
        private final long id;
        private final String name;
        private final String region;

        private ReserveOption(long id, String name, String region) {
            this.id = id;
            this.name = name;
            this.region = region;
        }

        @Override
        public String toString() {
            return region == null || region.isEmpty() ? name : name + " • " + region;
        }
    }

    private static class PublicEvent {
        private final String type;
        private final String priority;
        private final String description;
        private final String createdAt;
        private final int mediaCount;

        private PublicEvent(String type, String priority, String description, String createdAt, int mediaCount) {
            this.type = type;
            this.priority = priority;
            this.description = description;
            this.createdAt = createdAt;
            this.mediaCount = mediaCount;
        }
    }

    private interface OnPositionSelected {
        void onSelected(int position);
    }

    private static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final OnPositionSelected callback;

        private SimpleItemSelectedListener(OnPositionSelected callback) {
            this.callback = callback;
        }

        @Override
        public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
            callback.onSelected(position);
        }

        @Override
        public void onNothingSelected(android.widget.AdapterView<?> parent) {
        }
    }
}
