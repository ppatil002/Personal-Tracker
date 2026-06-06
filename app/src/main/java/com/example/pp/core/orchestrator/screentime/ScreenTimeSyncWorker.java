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
    private static final String TAG = "ScreenTimeSyncWorker";
    private final ScreenTimeRepository repository;

    public ScreenTimeSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.repository = ((PPApplication) context.getApplicationContext()).getScreenTimeRepository();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Worker execution started.");
        updateLog("RUNNING", "Syncing Screen Time", "Refreshing last 24-48 hours of usage data.");
        
        try {
            long end = System.currentTimeMillis();
            // We sync from the start of the previous day to now (24-48 hour sliding window).
            // This ensures we always capture the 'RESUMED' event for any session that might be active or crossing midnight.
            long start = DateUtils.getStartOfDay(end) - (24 * 60 * 60 * 1000L);
            
            repository.syncScreenTimeSync(start, end);

            updateLog("SUCCESS", "Syncing Screen Time", "Usage data updated successfully.");
            completeTask();
//            Log.i(TAG, "Worker execution completed successfully.");
            return Result.success();
        } catch (Exception e) {
            updateLog("FAILED", "Syncing Screen Time", "Error: " + e.getMessage());
            failTask(e.getMessage());
            return Result.failure();
        }
    }
    
    /*
    TECHNICAL ARCHITECTURE EXPLANATION:
    
    1. Hourly Bucketing & Splitting:
       Usage is stored in 1-hour slots. If a session crosses an hour boundary (e.g., 8:55 to 9:05),
       the logic splits it: 5 mins go to the 8:00 bucket, and 5 mins go to the 9:00 bucket.
       - TimesOpened: Increment is recorded in the bucket where the "RESUMED" event occurred (8:00).
    
    2. The "Missing Resume" Boundary Problem:
       UsageStatsManager provides a stream of events. To calculate duration, we MUST see the "RESUMED" event.
       - Case: App opens at 23:55 (Day 1) and closes at 00:05 (Day 2).
       - If the sync window starts at 00:00 (Day 2), we see the "PAUSED" event but miss the "RESUMED" event.
       - Result: Without the start event, the worker cannot identify the app or calculate duration. Usage is lost.
    
    3. The 24-48 Hour Sliding Window Solution:
       By starting the query from the beginning of the PREVIOUS day, we guarantee the window always starts 
       at a point where the phone was likely idle (midnight), ensuring we capture the "RESUMED" event 
       for any session currently active or crossing midnight boundaries.
    
    4. Data Integrity via MAX() Logic (Selective Upsert):
       Since the window overlaps (we re-process the same events multiple times), we must prevent data inflation.
       - Database Schema: Uses a Composite Primary Key (packageName + hourStartTimestamp).
       - Conflict Strategy: Instead of a simple REPLACE, we use a custom UPSERT:
         "durationMillis = MAX(durationMillis, EXCLUDED.durationMillis)"
       
       Why MAX()?
       - Correction: If a sync runs while an app is OPEN (8:55-9:00), it saves 5 mins. 
         If the next sync runs after it CLOSES (8:55-9:05), it calculates 10 mins. 
         MAX(5, 10) updates the DB to the correct 10 mins.
       - Boundary Protection: If a session eventually falls out of the sliding window (e.g., 7 days later), 
         a sync might only see the "tail end" of an old session and calculate a smaller duration (e.g., 2 mins).
         MAX(15, 2) ensures the original, full 15-minute record is PRESERVED and not overwritten by partial data.
    */
}
