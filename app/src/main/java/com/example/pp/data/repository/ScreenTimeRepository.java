package com.example.pp.data.repository;

import android.app.Application;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.data.local.entity.ScreenTime;
import com.example.pp.utils.DateUtils;
import com.example.pp.utils.ScreenUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ScreenTimeRepository {
    private static final String TAG = "ScreenTimeRepository PP";
    private final ScreenTimeDao screenTimeDao;
    private final UsageStatsManager usageStatsManager;
    private final AppExecutors executors;
    private final Context context;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    private final Set<String> launcherPackages = new HashSet<>();

    public ScreenTimeRepository(Application application) {
        this.context = application.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(application);
        this.screenTimeDao = db.screenTimeDao();
        this.usageStatsManager = (UsageStatsManager) application.getSystemService(Context.USAGE_STATS_SERVICE);
        this.executors = AppExecutors.getInstance();
        refreshLauncherPackages();
    }

    private void refreshLauncherPackages() {
        launcherPackages.clear();
        launcherPackages.addAll(ScreenUtils.getLauncherPackages(context));
        Log.d(TAG, "Identified " + launcherPackages.size() + " launcher/system packages to exclude.");
    }

    private boolean isExcluded(String pkg) {
        return pkg == null || launcherPackages.contains(pkg);
    }

    /**
     * Syncs screen time data from UsageStatsManager into local database.
     * Uses a raw event stream consumption pattern for high accuracy.
     */
    public void syncScreenTimeSync(long startTime, long endTime) {
        Log.d(TAG, "syncScreenTimeSync started: [" + sdf.format(new Date(startTime)) + "] to [" + sdf.format(new Date(endTime)) + "]");
        refreshLauncherPackages();

        if (usageStatsManager == null) {
            Log.e(TAG, "UsageStatsManager is NULL.");
            return;
        }

        UsageEvents events = usageStatsManager.queryEvents(startTime, endTime);
        if (events == null) {
            Log.e(TAG, "UsageEvents is NULL.");
            return;
        }

        Map<String, Map<Long, ScreenTimeEntryData>> bucketedUsage = new HashMap<>();
        String currentPkg = null;
        long currentStartTime = 0L;

        UsageEvents.Event event = new UsageEvents.Event();
        int eventCount = 0;
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            eventCount++;
            String pkg = event.getPackageName();
            int type = event.getEventType();
            long timestamp = event.getTimeStamp();

            switch (type) {
                case UsageEvents.Event.ACTIVITY_RESUMED:
                    // If an app was already "running", close its segment before starting new app
                    if (currentPkg != null) {
                        if (!isExcluded(currentPkg)) {
                            addUsageToBuckets(bucketedUsage, currentPkg, currentStartTime, timestamp);
                        }
                    }
                    currentPkg = pkg;
                    currentStartTime = timestamp;
                    if (!isExcluded(pkg)) {
                        incrementOpenCount(bucketedUsage, pkg, timestamp);
                    }
                    break;

                case UsageEvents.Event.ACTIVITY_PAUSED:
                    if (currentPkg != null && currentPkg.equals(pkg)) {
                        if (!isExcluded(currentPkg)) {
                            addUsageToBuckets(bucketedUsage, currentPkg, currentStartTime, timestamp);
                        }
                        currentPkg = null;
                        currentStartTime = 0L;
                    }
                    break;

                case UsageEvents.Event.KEYGUARD_SHOWN:
                case UsageEvents.Event.SCREEN_NON_INTERACTIVE:
                    // Stop tracking any active app when screen turns off or locks
                    if (currentPkg != null) {
                        if (!isExcluded(currentPkg)) {
                            addUsageToBuckets(bucketedUsage, currentPkg, currentStartTime, timestamp);
                        }
                        currentPkg = null;
                        currentStartTime = 0L;
                    }
                    break;

                case 12: // UsageEvents.Event.NOTIFICATION_INTERRUPTION
                    if (!isExcluded(pkg)) {
                        incrementNotificationCount(bucketedUsage, pkg, timestamp);
                    }
                    break;
            }
        }

        // Handle app still open at the end of the query range
        if (currentPkg != null && currentStartTime > 0 && endTime > currentStartTime) {
            if (!isExcluded(currentPkg)) {
                addUsageToBuckets(bucketedUsage, currentPkg, currentStartTime, endTime);
            }
        }

        Log.d(TAG, "Processed " + eventCount + " UsageEvents.");
        if (!bucketedUsage.isEmpty()) {
            saveBucketedEntries(bucketedUsage);
        }
    }

    private void incrementOpenCount(Map<String, Map<Long, ScreenTimeEntryData>> bucketedUsage, String pkg, long timestamp) {
        long hourStart = DateUtils.getStartOfHour(timestamp);
        ScreenTimeEntryData data = getOrCreateBucket(bucketedUsage, pkg, hourStart);
        data.opens++;
    }

    private void incrementNotificationCount(Map<String, Map<Long, ScreenTimeEntryData>> bucketedUsage, String pkg, long timestamp) {
        long hourStart = DateUtils.getStartOfHour(timestamp);
        ScreenTimeEntryData data = getOrCreateBucket(bucketedUsage, pkg, hourStart);
        data.notifications++;
    }

    private ScreenTimeEntryData getOrCreateBucket(Map<String, Map<Long, ScreenTimeEntryData>> bucketedUsage, String pkg, long hourStart) {
        Map<Long, ScreenTimeEntryData> pkgBuckets = bucketedUsage.computeIfAbsent(pkg, k -> new HashMap<>());
        return pkgBuckets.computeIfAbsent(hourStart, k -> new ScreenTimeEntryData());
    }

    private void addUsageToBuckets(Map<String, Map<Long, ScreenTimeEntryData>> bucketedUsage, String pkg, long start, long end) {
        if (end <= start) return;
        long current = start;
        while (current < end) {
            long hourStart = DateUtils.getStartOfHour(current);
            long hourEnd = hourStart + 3600000L;
            long segmentEnd = Math.min(end, hourEnd);
            long segmentLength = segmentEnd - current;

            if (segmentLength > 0) {
                ScreenTimeEntryData data = getOrCreateBucket(bucketedUsage, pkg, hourStart);
                data.duration += segmentLength;
            }
            current = segmentEnd;
        }
    }

    private void saveBucketedEntries(Map<String, Map<Long, ScreenTimeEntryData>> bucketedUsage) {
        List<ScreenTime> allToInsert = new ArrayList<>();
        for (Map.Entry<String, Map<Long, ScreenTimeEntryData>> pkgEntry : bucketedUsage.entrySet()) {
            String pkg = pkgEntry.getKey();
            for (Map.Entry<Long, ScreenTimeEntryData> hourEntry : pkgEntry.getValue().entrySet()) {
                ScreenTime st = new ScreenTime(pkg, hourEntry.getKey(), hourEntry.getValue().duration);
                st.setTimesOpened(hourEntry.getValue().opens);
                st.setNotificationsReceived(hourEntry.getValue().notifications);
                allToInsert.add(st);
            }
        }
        if (!allToInsert.isEmpty()) {
            screenTimeDao.insertAll(allToInsert);
            Log.i(TAG, "Sync complete: Updated " + allToInsert.size() + " hourly entries.");
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
}
