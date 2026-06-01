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

    /**
     * Syncs screen time data from UsageStatsManager into local database.
     * Use background thread when calling this.
     */
    public void syncScreenTimeSync(long startTime, long endTime) {
        Log.d(TAG, "syncScreenTimeSync started: [" + sdf.format(new Date(startTime)) + "] to [" + sdf.format(new Date(endTime)) + "]");

        if (usageStatsManager == null) {
            Log.e(TAG, "UsageStatsManager is NULL.");
            return;
        }

        UsageEvents events = usageStatsManager.queryEvents(startTime, endTime);
        if (events == null) {
            Log.e(TAG, "UsageEvents is NULL.");
            return;
        }

        // Map<PackageName, Map<HourStartTimestamp, ScreenTimeEntryData>>
        Map<String, Map<Long, ScreenTimeEntryData>> bucketedUsage = new HashMap<>();
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
                long hourStart = DateUtils.getStartOfHour(timestamp);
                Map<Long, ScreenTimeEntryData> pkgBuckets = bucketedUsage.computeIfAbsent(packageName, k -> new HashMap<>());
                ScreenTimeEntryData data = pkgBuckets.get(hourStart);
                if (data == null) {
                    data = new ScreenTimeEntryData();
                    pkgBuckets.put(hourStart, data);
                }
                data.opens++;
            } else if (eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                Long start = openTimestamps.remove(packageName);
                if (start != null) {
                    long duration = timestamp - start;
                    if (duration > 0) {
                        addUsageToBuckets(bucketedUsage, packageName, start, timestamp);
                    }
                }
            } else if (eventType == 12) { // UsageEvents.Event.NOTIFICATION_INTERRUPTION = 12
                long hourStart = DateUtils.getStartOfHour(timestamp);
                Map<Long, ScreenTimeEntryData> pkgBuckets = bucketedUsage.computeIfAbsent(packageName, k -> new HashMap<>());
                ScreenTimeEntryData data = pkgBuckets.get(hourStart);
                if (data == null) {
                    data = new ScreenTimeEntryData();
                    pkgBuckets.put(hourStart, data);
                }
                data.notifications++;
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

    private void addUsageToBuckets(Map<String, Map<Long, ScreenTimeEntryData>> bucketedUsage, String pkg, long start, long end) {
        long current = start;
        while (current < end) {
            long hourStart = DateUtils.getStartOfHour(current);
            long hourEnd = hourStart + 3600000L;
            long sessionEndInHour = Math.min(end, hourEnd);
            long durationInHour = sessionEndInHour - current;
            
            if (durationInHour > 0) {
                Map<Long, ScreenTimeEntryData> pkgBuckets = bucketedUsage.computeIfAbsent(pkg, k -> new HashMap<>());
                ScreenTimeEntryData data = pkgBuckets.get(hourStart);
                if (data == null) {
                    data = new ScreenTimeEntryData();
                    pkgBuckets.put(hourStart, data);
                }
                data.duration += durationInHour;
            }
            current = hourEnd;
        }
    }

    private void saveBucketedEntries(Map<String, Map<Long, ScreenTimeEntryData>> bucketedUsage) {
        List<ScreenTime> allToInsert = new ArrayList<>();
        for (Map.Entry<String, Map<Long, ScreenTimeEntryData>> pkgEntry : bucketedUsage.entrySet()) {
            String pkg = pkgEntry.getKey();
            for (Map.Entry<Long, ScreenTimeEntryData> hourEntry : pkgEntry.getValue().entrySet()) {
                long hourStart = hourEntry.getKey();
                ScreenTimeEntryData entryData = hourEntry.getValue();
                
                screenTimeDao.deleteEntry(pkg, hourStart);
                ScreenTime st = new ScreenTime(pkg, hourStart, entryData.duration);
                st.setTimesOpened(entryData.opens);
                st.setNotificationsReceived(entryData.notifications);
                allToInsert.add(st);
            }
        }
        
        if (!allToInsert.isEmpty()) {
            screenTimeDao.insertAll(allToInsert);
            Log.i(TAG, "Successfully inserted/updated " + allToInsert.size() + " hourly entries.");
        }
    }

    private static class ScreenTimeEntryData {
        long duration = 0;
        int opens = 0;
        int notifications = 0;
    }

    public void syncScreenTime(long startTime, long endTime) {
        executors.diskIO().execute(() -> syncScreenTimeSync(startTime, endTime));
    }

    public LiveData<List<ScreenTimeDao.AppUsageSummary>> getTodayAppUsage(long startOfDay) {
        return screenTimeDao.getAppUsageSummary(startOfDay, System.currentTimeMillis() + 86400000L);
    }

    public LiveData<List<ScreenTimeDao.HourlyUsageSummary>> getHourlyUsage(long start, long end, String packageName) {
        return screenTimeDao.getHourlyUsageSummary(start, end, packageName);
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
