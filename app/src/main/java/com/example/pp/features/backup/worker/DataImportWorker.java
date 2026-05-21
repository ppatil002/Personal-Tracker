package com.example.pp.features.backup.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.example.pp.data.local.AppDatabase;
import com.example.pp.features.backup.model.BackupData;
import com.example.pp.infra.background.BaseFeatureWorker;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;

public class DataImportWorker extends BaseFeatureWorker {

    public DataImportWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String restoredFilePath = getInputData().getString("RESTORED_FILE_PATH");
        if (restoredFilePath == null) {
            updateLog("FAILED", "IMPORT_ERROR", "Restored file path missing.");
            return Result.failure();
        }

        updateLog("RUNNING", "IMPORTING", "Merging restored JSON data into local database.");

        try {
            File restoredFile = new File(restoredFilePath);
            Gson gson = new Gson();
            BackupData backupData;
            try (FileReader reader = new FileReader(restoredFile)) {
                backupData = gson.fromJson(reader, BackupData.class);
            }

            if (backupData == null) {
                throw new Exception("Failed to parse restored data.");
            }

            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            db.runInTransaction(() -> {
                // 1. Import Categories
                if (backupData.categories != null && !backupData.categories.isEmpty()) {
                    db.categoryDao().upsertAll(backupData.categories);
                }
                
                // 2. Import Tags
                if (backupData.tags != null && !backupData.tags.isEmpty()) {
                    db.tagDao().upsertAll(backupData.tags);
                }

                // 3. Import Transactions
                if (backupData.transactions != null && !backupData.transactions.isEmpty()) {
                    db.transactionDao().upsertAll(backupData.transactions);
                }

                // 4. Import Cross-refs
                if (backupData.transactionTags != null && !backupData.transactionTags.isEmpty()) {
                    db.transactionDao().upsertAllTagCrossRefs(backupData.transactionTags);
                }
            });

            updateLog("SUCCESS", "IMPORT_COMPLETE", "Database successfully updated from backup.");
            
            // Cleanup the decrypted file
            if (restoredFile.exists()) {
                restoredFile.delete();
            }

            return Result.success();

        } catch (Exception e) {
            updateLog("FAILED", "IMPORT_ERROR", "Import failed: " + e.getMessage());
            return Result.failure();
        }
    }
}
