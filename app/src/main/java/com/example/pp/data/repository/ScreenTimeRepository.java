package com.example.pp.data.repository;

import android.app.AppOpsManager;
import android.app.Application;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Process;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.data.local.entity.ScreenTime;
import com.example.pp.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScreenTimeRepository {
    private static final String TAG = "ScreenTimeRepository PP";
    private final ScreenTimeDao screenTimeDao;
    private final UsageStatsManager usageStatsManager;
    private final AppExecutors executors;
    private final Context context;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public ScreenTimeRepository(Application application) {
        this.context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(application);
        this.screenTimeDao = db.screenTimeDao();
        this.usageStatsManager = (UsageStatsManager) application.getSystemService(Context.USAGE_STATS_SERVICE);
        this.executors = AppExecutors.getInstance();
    }

    private boolean hasPermission() {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    /**
     * Syncs screen time data from UsageStatsManager into local database.
     * Use background thread when calling this.
     */
    public void syncScreenTimeSync(long startTime, long endTime) {
        Log.d(TAG, "syncScreenTimeSync started: [" + sdf.format(new Date(startTime)) + "] to [" + sdf.format(new Date(endTime)) + "]");
        
        if (!hasPermission()) {
            Log.e(TAG, "CRITICAL: PACKAGE_USAGE_STATS permission not granted! Querying will return 0 events.");
            return;
        }

        if (usageStatsManager == null) {
            Log.e(TAG, "UsageStatsManager is NULL.");
            return;
        }

        UsageEvents events = usageStatsManager.queryEvents(startTime, endTime);
        if (events == null) {
            Log.e(TAG, "UsageEvents is NULL.");
            return;
        }

        // DEBUG: Check if queryUsageStats returns anything
        List<android.app.usage.UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        if (stats != null) {
            Log.d(TAG, "queryUsageStats returned " + stats.size() + " entries as fallback check.");
        } else {
            Log.w(TAG, "queryUsageStats also returned NULL/Empty.");
        }

        // Map<PackageName, Map<HourStartTimestamp, Duration>>
        Map<String, Map<Long, Long>> bucketedUsage = new HashMap<>();
        Map<String, Long> openTimestamps = new HashMap<>();
        
        UsageEvents.Event event = new UsageEvents.Event();
        int eventCount = 0;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            eventCount++;
            String packageName = event.getPackageName();
            int eventType = event.getEventType();
            long timestamp = event.getTimeStamp();

            if (eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                openTimestamps.put(packageName, timestamp);
            } else if (eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                Long start = openTimestamps.remove(packageName);
                if (start != null) {
                    long duration = timestamp - start;
                    if (duration > 0) {
                        addUsageToBuckets(bucketedUsage, packageName, start, timestamp);
                    }
                }
            }
        }
        
        Log.d(TAG, "Processed " + eventCount + " UsageEvents.");
        
        // Handle apps still open at the end
        for (Map.Entry<String, Long> entry : openTimestamps.entrySet()) {
            long start = entry.getValue();
            if (endTime > start) {
                addUsageToBuckets(bucketedUsage, entry.getKey(), start, endTime);
            }
        }

        if (bucketedUsage.isEmpty()) {
            Log.w(TAG, "No app usage bucketed in the given range.");
        } else {
            Log.d(TAG, "Found usage for " + bucketedUsage.size() + " apps. Saving to DB...");
            saveBucketedEntries(bucketedUsage);
        }
    }

    private void addUsageToBuckets(Map<String, Map<Long, Long>> bucketedUsage, String pkg, long start, long end) {
        long current = start;
        while (current < end) {
            long hourStart = DateUtils.getStartOfHour(current);
            long hourEnd = hourStart + 3600000L;
            long sessionEndInHour = Math.min(end, hourEnd);
            long durationInHour = sessionEndInHour - current;
            
            if (durationInHour > 0) {
                Map<Long, Long> pkgBuckets = bucketedUsage.computeIfAbsent(pkg, k -> new HashMap<>());
                pkgBuckets.put(hourStart, pkgBuckets.getOrDefault(hourStart, 0L) + durationInHour);
            }
            current = hourEnd;
        }
    }

    private void saveBucketedEntries(Map<String, Map<Long, Long>> bucketedUsage) {
        List<ScreenTime> allToInsert = new ArrayList<>();
        for (Map.Entry<String, Map<Long, Long>> pkgEntry : bucketedUsage.entrySet()) {
            String pkg = pkgEntry.getKey();
            for (Map.Entry<Long, Long> hourEntry : pkgEntry.getValue().entrySet()) {
                long hourStart = hourEntry.getKey();
                long duration = hourEntry.getValue();
                
                // We update existing records by summing if we wanted to be perfectly additive,
                // but since we are re-syncing the whole day, we delete and replace for the hour.
                // NOTE: If we delete hour by hour, we might lose data if we only sync a partial hour.
                // Better: query current duration from DB and take MAX(system, db) or just trust system since we query entire range.
                screenTimeDao.deleteEntry(pkg, hourStart);
                allToInsert.add(new ScreenTime(pkg, hourStart, duration));
                Log.v(TAG, "Bucketed: " + pkg + " | hour: " + sdf.format(new Date(hourStart)) + " | duration: " + duration + "ms");
            }
        }
        
        if (!allToInsert.isEmpty()) {
            screenTimeDao.insertAll(allToInsert);
            Log.i(TAG, "Successfully inserted/updated " + allToInsert.size() + " hourly entries.");
        }
    }

    public void syncScreenTime(long startTime, long endTime) {
        executors.diskIO().execute(() -> syncScreenTimeSync(startTime, endTime));
    }

    public LiveData<List<ScreenTimeDao.AppUsageSummary>> getTodayAppUsage(long startOfDay) {
        return screenTimeDao.getAppUsageSummary(startOfDay, System.currentTimeMillis() + 86400000L);
    }

    public LiveData<List<ScreenTimeDao.HourlyUsageSummary>> getHourlyUsage(long start, long end) {
        return screenTimeDao.getHourlyUsageSummary(start, end);
    }
    
    public LiveData<List<ScreenTimeDao.AppUsageSummary>> getAppUsageRange(long start, long end) {
        return screenTimeDao.getAppUsageSummary(start, end);
    }

    public void insertSession(String packageName, long startTime, long endTime) {
        executors.diskIO().execute(() -> {
            long duration = endTime - startTime;
            if (duration <= 0) return;
            
            long hourStart = DateUtils.getStartOfHour(startTime);
            Log.d(TAG, "Manually inserting session: " + packageName + " | " + duration + "ms");
            ScreenTime entry = new ScreenTime(packageName, hourStart, duration);
            screenTimeDao.insert(entry); 
        });
    }
}
