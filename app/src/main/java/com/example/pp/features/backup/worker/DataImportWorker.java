package com.example.pp.features.backup.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.example.pp.PPApplication;
import com.example.pp.core.cache.CategoryCacheManager;
import com.example.pp.core.cache.TagCacheManager;
import com.example.pp.core.orchestrator.BaseFeatureWorker;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.model.SyncStats;
import com.example.pp.features.backup.model.BackupData;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

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
            List<SyncStats> allStats = new ArrayList<>();

            db.runInTransaction(() -> {
                allStats.add(db.categoryDao().upsertAll(backupData.categories));
                allStats.add(db.tagDao().upsertAll(backupData.tags));
                allStats.add(db.transactionDao().upsertAll(backupData.transactions));
                allStats.add(db.transactionDao().upsertAllTagCrossRefs(backupData.transactionTags));
                allStats.add(db.taskDao().upsertAllDefinitions(backupData.taskDefinitions));
                allStats.add(db.taskDao().upsertAllStates(backupData.taskStates));
                allStats.add(db.backgroundJobLogDao().upsertAll(backupData.backgroundJobLogs));
                allStats.add(db.hydrationDao().upsertAll(backupData.hydrationIntakes));
                allStats.add(db.nutrientDao().upsertAll(backupData.nutrientData));
                allStats.add(db.nutritionIntakeDao().upsertAll(backupData.nutritionIntakes));
                allStats.add(db.sleepDao().upsertAll(backupData.sleep));
                allStats.add(db.screenTimeDao().upsertAll(backupData.screenTime));
                allStats.add(db.historyDao().upsertAll(backupData.history));
                allStats.add(db.quotesDao().upsertAll(backupData.quotes));
                allStats.add(db.quotesDao().upsertAllSavedQuotes(backupData.savedQuotes));
                allStats.add(db.gymDao().upsertAllWeights(backupData.bodyWeight));
                allStats.add(db.gymDao().upsertAllGymLogs(backupData.gymTracking));
                allStats.add(db.vocabularyDao().upsertAll(backupData.vocabulary));
            });

            StringBuilder summary = new StringBuilder("Restore Summary:\n");
            for (SyncStats s : allStats) {
                summary.append(s.toString()).append("\n\n");
            }

            updateLog("SUCCESS", "IMPORT_COMPLETE", summary.toString());
            
            // Invalidate caches after successful restore
            CategoryCacheManager.getInstance().setDirty(true);
            TagCacheManager.getInstance().setDirty(true);
            
            PPApplication app = (PPApplication) getApplicationContext();
            app.getCategoryRepository().refreshCategoryTree();
            app.getTagRepository().refreshTags();
            
            if (restoredFile.exists()) {
                restoredFile.delete();
            }
            
            Data outputData = new Data.Builder()
                    .putString("IMPORT_SUMMARY", summary.toString())
                    .build();

            return Result.success(outputData);

        } catch (Exception e) {
            updateLog("FAILED", "IMPORT_ERROR", "Import failed: " + e.getMessage());
            return Result.failure();
        }
    }
}
