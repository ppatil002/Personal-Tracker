package com.example.pp.features.backup.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.example.pp.PPApplication;
import com.example.pp.core.orchestrator.BaseFeatureWorker;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.repository.BackgroundJobLogRepository;
import com.example.pp.features.backup.model.BackupData;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;

public class DataExportWorker extends BaseFeatureWorker {

    public DataExportWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        updateLog("RUNNING", "EXPORTING", "Serializing local SQLite tables to JSON stream.");
        updateTaskProgress(33);

        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            BackgroundJobLogRepository backgroundJobLogRepository = ((PPApplication) getApplicationContext())
                .getBackgroundJobLogRepository();
            
            BackupData backupData = new BackupData(
                db.categoryDao().getAllCategoriesList(),
                db.tagDao().getAllTagsList(),
                db.transactionDao().getAllTransactionsList(),
                db.transactionDao().getAllTransactionTagCrossRefs(),
                db.taskDao().getAllDefinitions(),
                db.taskDao().getAllStates(),
                backgroundJobLogRepository.getAllLogsList(),
                db.hydrationDao().getAllIntakes(),
                db.nutrientDao().getAllList(),
                db.nutritionDao().getAllIntakes(),
                db.sleepDao().getAllSleepEntries(),
                db.screenTimeDao().getAllEntriesSync()
            );

            Gson gson = new Gson();
            String json = gson.toJson(backupData);

            File exportFile = new File(getApplicationContext().getCacheDir(), "temp_raw.json");
            try (FileWriter writer = new FileWriter(exportFile)) {
                writer.write(json);
            }

            updateLog("SUCCESS", "EXPORT_COMPLETE", "Local data exported successfully.");
            
            Data outputData = new Data.Builder()
                    .putString("RAW_FILE_PATH", exportFile.getAbsolutePath())
                    .putString("TASK_ID", taskId)
                    .build();
            
            return Result.success(outputData);
            
        } catch (Exception e) {
            updateLog("FAILED", "EXPORT_ERROR", e.getMessage());
            failTask(e.getMessage());
            return Result.failure(new Data.Builder().putString("ERROR_DETAIL", e.getMessage()).build());
        }
    }
}
