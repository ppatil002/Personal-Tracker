package com.example.pp.infra.background;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkerParameters;

import com.example.pp.features.backup.worker.CloudUploadWorker;
import com.example.pp.features.backup.worker.CryptoStreamWorker;
import com.example.pp.features.backup.worker.DataExportWorker;

import java.util.UUID;

public class MainOrchestratorWorker extends BaseFeatureWorker {

    public MainOrchestratorWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        updateLog("RUNNING", "INITIALIZING", "Pre-flight checks passed. Initializing chained pipelines.");

        try {
            WorkManager workManager = WorkManager.getInstance(getApplicationContext());
            String syncId = UUID.randomUUID().toString();

            Data dataPayload = new Data.Builder()
                    .putString("EXECUTION_SESSION_ID", syncId)
                    .build();

            OneTimeWorkRequest exportTask = new OneTimeWorkRequest.Builder(DataExportWorker.class).setInputData(dataPayload).build();
            OneTimeWorkRequest cryptoTask = new OneTimeWorkRequest.Builder(CryptoStreamWorker.class).setInputData(dataPayload).build();
            OneTimeWorkRequest uploadTask = new OneTimeWorkRequest.Builder(CloudUploadWorker.class).setInputData(dataPayload).build();

            workManager.beginWith(exportTask)
                       .then(cryptoTask)
                       .then(uploadTask)
                       .enqueue();

            updateLog("SUCCESS", "ENQUEUED", "All background worker pipelines successfully enqueued.");
            return Result.success();
            
        } catch (Exception e) {
            updateLog("FAILED", "ERROR", "Critical pipeline execution error: " + e.getMessage());
            return Result.failure();
        }
    }
}