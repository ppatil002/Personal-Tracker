package com.example.pp.features.backup.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.example.pp.BuildConfig;
import com.example.pp.data.remote.GoogleDriveClient;
import com.example.pp.infra.background.BaseFeatureWorker;

import java.io.File;

public class CloudDownloadWorker extends BaseFeatureWorker {

    public CloudDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String remoteName = getInputData().getString("REMOTE_FILE_NAME");
        if (remoteName == null) {
            updateLog("FAILED", "DOWNLOAD_ERROR", "Remote file name missing.");
            return Result.failure();
        }

        updateLog("RUNNING", "DOWNLOADING", "Pulling encrypted payload from Google Drive.");

        try {
            GoogleDriveClient driveClient = GoogleDriveClient.getInstance(getApplicationContext());
            if (!driveClient.authorizeSync(getApplicationContext())) {
                updateLog("FAILED", "DOWNLOAD_ERROR", "Google Drive not authorized.");
                return Result.failure();
            }

            File backupDir = new File(getApplicationContext().getExternalFilesDir(null), BuildConfig.BACKUP_PATH);

            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            File localFile = new File(backupDir, "downloaded_" + remoteName);
            driveClient.downloadFile(remoteName, localFile);

            updateLog("SUCCESS", "DOWNLOAD_COMPLETE", "Backup downloaded to: " + localFile.getAbsolutePath());
            Data outputData = new Data.Builder()
                    .putString("ENCRYPTED_FILE_PATH", localFile.getAbsolutePath())
                    .build();
            return Result.success(outputData);

        } catch (Exception e) {
            updateLog("FAILED", "DOWNLOAD_ERROR", "Critical download error: " + e.getMessage());
            return Result.failure();
        }
    }
}
