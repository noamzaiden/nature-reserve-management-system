package com.reserve.mobile;

import android.app.Activity;
import android.net.Uri;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ReportMediaController {

    private static final String[] REPORT_MEDIA_MIME_TYPES = {"image/*", "video/*"};

    private final Activity activity;
    private final TextView selectedMediaText;
    private final List<Uri> selectedMediaUris = new ArrayList<>();
    private Uri pendingCameraPhotoUri;

    ReportMediaController(Activity activity, TextView selectedMediaText) {
        this.activity = activity;
        this.selectedMediaText = selectedMediaText;
    }

    // Launches the picker for photo/video attachments.
    void openMediaPicker(ActivityResultLauncher<String[]> mediaPickerLauncher) {
        mediaPickerLauncher.launch(REPORT_MEDIA_MIME_TYPES);
    }

    // Applies selected media returned from the picker.
    void onMediaPicked(List<Uri> uris) {
        if (uris != null) {
            selectedMediaUris.addAll(uris);
        }
        updateSelectedMediaText();
    }

    // Prepares a photo Uri and returns it for camera capture.
    Uri prepareCameraCapture() {
        pendingCameraPhotoUri = createCameraImageUri();
        return pendingCameraPhotoUri;
    }

    // Handles the result of a camera capture attempt.
    void onCameraCaptureResult(boolean success) {
        if (success && pendingCameraPhotoUri != null) {
            selectedMediaUris.add(pendingCameraPhotoUri);
            showShortToast(R.string.camera_attachment_added);
        } else {
            showShortToast(R.string.camera_capture_cancelled);
        }
        pendingCameraPhotoUri = null;
        updateSelectedMediaText();
    }

    // Clears all selected media so the next report starts fresh.
    void clearSelectedMedia() {
        selectedMediaUris.clear();
        pendingCameraPhotoUri = null;
        updateSelectedMediaText();
    }

    List<Uri> getSelectedMediaUris() {
        return Collections.unmodifiableList(selectedMediaUris);
    }

    void updateSelectedMediaText() {
        int attachmentCount = selectedMediaUris.size();
        if (attachmentCount == 0) {
            selectedMediaText.setText(R.string.selected_media_none);
            return;
        }
        selectedMediaText.setText(activity.getResources().getQuantityString(
                R.plurals.selected_media_count,
                attachmentCount,
                attachmentCount
        ));
    }

    private Uri createCameraImageUri() {
        File picturesDir = new File(activity.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), "reports");
        if (!picturesDir.exists() && !picturesDir.mkdirs()) {
            throw new IllegalStateException("Could not create report picture directory");
        }
        File photoFile = new File(picturesDir, "report_" + System.currentTimeMillis() + ".jpg");
        return FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", photoFile);
    }

    private void showShortToast(int messageResId) {
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show();
    }
}
