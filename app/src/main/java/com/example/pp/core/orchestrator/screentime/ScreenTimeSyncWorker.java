package com.example.pp.core.orchestrator.screentime;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.example.pp.PPApplication;
import com.example.pp.core.orchestrator.BaseFeatureWorker;
import com.example.pp.data.repository.ScreenTimeRepository;
import com.example.pp.utils.DateUtils;

public class ScreenTimeSyncWorker extends BaseFeatureWorker {
    private static final String TAG = "ScreenTimeSyncWorker PP";
    private final ScreenTimeRepository repository;

    public ScreenTimeSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.repository = ((PPApplication) context.getApplicationContext()).getScreenTimeRepository();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Worker execution started.");
        updateLog("RUNNING", "Syncing Screen Time", "Starting data collection from UsageStatsManager");
        
        try {
            long end = System.currentTimeMillis();
            // Sync for the entire current day up to now. 
            // We query once to ensure sessions crossing hour boundaries are handled correctly in the repository.
            long start = DateUtils.getStartOfDay(end);
            
            Log.d(TAG, "Syncing entire day: [" + start + "] to [" + end + "]");
            
            repository.syncScreenTimeSync(start, end);

            updateLog("COMPLETED", "Syncing Screen Time", "Data collection finished successfully");
            completeTask();
            Log.i(TAG, "Worker execution completed successfully.");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Screen time sync failed", e);
            updateLog("FAILED", "Syncing Screen Time", "Error: " + e.getMessage());
            failTask(e.getMessage());
            return Result.failure();
        }
    }
}
