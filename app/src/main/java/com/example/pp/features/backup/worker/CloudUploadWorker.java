package com.example.pp.features.backup.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.example.pp.core.orchestrator.BaseFeatureWorker;
import com.example.pp.data.remote.GoogleDriveClient;

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
            updateLog("FAILED", "UPLOAD_ERROR", "Data missing from input pipeline.");
            failTask("Missing file path or name in upload stage.");
            return Result.failure();
        }

        updateLog("RUNNING", "UPLOADING", "Streaming stream to Google Drive.");
        updateTaskProgress(90);

        try {
            GoogleDriveClient driveClient = GoogleDriveClient.getInstance(getApplicationContext());
            if (!driveClient.authorizeSync(getApplicationContext())) {
                updateLog("FAILED", "UPLOAD_ERROR", "Authentication failed during background sync.");
                failTask("Google Drive authentication failed.");
                return Result.failure();
            }

            File file = new File(encryptedPath);
            String fileId = driveClient.uploadFile(file, remoteName);

            if (fileId != null) {
                updateLog("SUCCESS", "UPLOAD_COMPLETE", "Backup successful. File ID: " + fileId);
                completeTask();
                return Result.success();
            } else {
                updateLog("FAILED", "UPLOAD_ERROR", "Upload failed: Unknown error.");
                failTask("Google Drive upload failed.");
                return Result.failure();
            }

        } catch (Exception e) {
            updateLog("FAILED", "UPLOAD_ERROR", e.getMessage());
            failTask(e.getMessage());
            return Result.failure();
        }
    }
}
