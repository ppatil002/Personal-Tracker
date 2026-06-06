package com.example.pp.features.screentime.service;

import static com.example.pp.core.constants.AppConstants.SCREEN_WORKER;

import android.accessibilityservice.AccessibilityService;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.orchestrator.screentime.ScreenTimeNotificationHelper;
import com.example.pp.core.orchestrator.screentime.ScreenTimeReceiver;
import com.example.pp.core.orchestrator.screentime.ScreenTimeScheduler;
import com.example.pp.core.preferences.PreferenceManager;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.utils.DateUtils;
import com.example.pp.utils.ScreenUtils;

import java.util.HashSet;
import java.util.Set;

public class ScreenTrackingService extends AccessibilityService {
    private static final String TAG = "ScreenTrackingService";

    private AlarmManager alarmManager;
    private PreferenceManager prefManager;
    private final Set<String> excludedPackages = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        prefManager = PreferenceManager.getInstance(this);
        refreshExcludedPackages();
    }

    private void refreshExcludedPackages() {
        excludedPackages.clear();
        excludedPackages.addAll(ScreenUtils.getLauncherPackages(this));
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "Accessibility Service Connected. Starting Foreground.");
        startForeground(AppConstants.NOTIFY_ID_SCREEN_TIME,
                ScreenTimeNotificationHelper.getServiceNotification(this));

        // Ensure the sync worker is scheduled whenever the service starts/reconnects
        ScreenTimeScheduler.schedule(this, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = event.getPackageName();
            if (pkg == null) return;
            
            String newPackage = pkg.toString();
            handleAppSwitch(newPackage);
        }
    }

    private void handleAppSwitch(String newPackage) {
        long now = System.currentTimeMillis();
        long sessionStartTime = prefManager.getLong(AppConstants.KEY_SCREEN_SESSION_START, 0);
        
//        Log.d(TAG, "App Switch -> Package: [" + newPackage + "] | Active Session Start: " + formatTime(sessionStartTime));

        if (isLockScreen(newPackage)) {
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            boolean isLocked = km != null && km.isKeyguardLocked();
            
            if (isLocked) {
//                Log.i(TAG, "REAL Lock Screen detected [" + newPackage + "]. Resetting wellness timer.");
                cancelBreakAlarm();
                addJobLog("LOCKED", "Screen Monitoring", "User locked device. Timer stopped.");
                prefManager.putLong(AppConstants.KEY_SCREEN_SESSION_START, 0);
                // Capture usage immediately on lock
                ScreenTimeScheduler.runNow(this, "LOCK_TRIGGER");
            }
        } else {
            if (sessionStartTime == 0) {
                // No session running. Should we start one?
                if (!isExcludedPackage(newPackage)) {
                    prefManager.putLong(AppConstants.KEY_SCREEN_SESSION_START, now);
                    scheduleBreakAlarm(AppConstants.SCREEN_BREAK_INTERVAL_MS);
                    addJobLog("STARTED", "Screen Monitoring", "App: " + newPackage + " | Alarm set for 20 mins.");
                }
            }
        }
    }

    private boolean isLockScreen(String packageName) {
        return packageName.equals("com.android.systemui") || 
               packageName.contains("keyguard") || 
               packageName.contains("lockscreen");
    }

    private boolean isExcludedPackage(String packageName) {
        return excludedPackages.contains(packageName) || 
               packageName.toLowerCase().contains("launcher");
    }

    private void scheduleBreakAlarm(long delayMs) {
        Intent intent = new Intent(this, ScreenTimeReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAtMillis = System.currentTimeMillis() + delayMs;
        if (alarmManager != null) {
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent);
            alarmManager.setAlarmClock(info, pendingIntent);
        }
    }

    private void cancelBreakAlarm() {
        Intent intent = new Intent(this, ScreenTimeReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    @Override
    public void onInterrupt() {
        addJobLog("INTERRUPTED", "Screen Monitoring", "Accessibility stream interrupted by system.");
    }

    @Override
    public void onDestroy() {
        addJobLog("DESTROYED", "Screen Monitoring", "Service is being destroyed.");
        super.onDestroy();
    }
    private void addJobLog(String status, String step, String message) {
        try {
            BackgroundJobLog log = new BackgroundJobLog(
                    TAG + "_" + DateUtils.convertTimestampintoHH_MM_SS_String(System.currentTimeMillis()),
                    SCREEN_WORKER,
                    status,
                    step,
                    message,
                    System.currentTimeMillis()
            );
            AppExecutors.getInstance().diskIO().execute(() -> {
                ((PPApplication) getApplicationContext())
                        .getBackgroundJobLogRepository()
                        .mergeDuplicateDailyWorkerLog(log);
            });
        } catch (Exception e) {
            Log.e(TAG, "Error adding background job log", e);
        }
    }
}
