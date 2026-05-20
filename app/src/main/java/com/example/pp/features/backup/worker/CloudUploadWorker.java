package com.example.pp.features.backup.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.example.pp.data.remote.GoogleDriveClient;
import com.example.pp.infra.background.BaseFeatureWorker;

import java.io.File;

public class CloudUploadWorker extends BaseFeatureWorker {

    public CloudUploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String encryptedPath = getInputData().getString("ENCRYPTED_FILE_PATH");
        String remoteName = getInputData().getString("REMOTE_FILE_NAME");

        if (encryptedPath == null || remoteName == null) {
            updateLog("FAILED", "UPLOAD_ERROR", "Encrypted file info missing from input.");
            return Result.failure();
        }

        updateLog("RUNNING", "UPLOADING", "Streaming encrypted payload to Google Drive.");

        try {
            GoogleDriveClient driveClient = GoogleDriveClient.getInstance(getApplicationContext());
            if (!driveClient.authorizeSync(getApplicationContext())) {
                updateLog("FAILED", "UPLOAD_ERROR", "Google Drive not authorized. Please sign in manually once.");
                return Result.failure();
            }

            File file = new File(encryptedPath);
            String fileId = driveClient.uploadFile(file, remoteName);

            if (fileId != null) {
                updateLog("SUCCESS", "UPLOAD_COMPLETE", "Backup successfully pushed to Google Drive. File ID: " + fileId);
                return Result.success();
            } else {
                updateLog("FAILED", "UPLOAD_ERROR", "Google Drive upload failed: Unknown error");
                return Result.failure();
            }

        } catch (Exception e) {
            updateLog("FAILED", "UPLOAD_ERROR", "Critical upload error: " + e.getMessage());
            return Result.failure();
        }
    }
}