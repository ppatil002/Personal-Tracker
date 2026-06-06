package com.example.pp.features.screentime.service;

import android.accessibilityservice.AccessibilityService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.orchestrator.screentime.ScreenTimeNotificationHelper;
import com.example.pp.core.orchestrator.screentime.ScreenTimeReceiver;
import com.example.pp.core.orchestrator.screentime.ScreenTimeScheduler;
import com.example.pp.core.preferences.PreferenceManager;
import com.example.pp.utils.ScreenUtils;

import java.util.HashSet;
import java.util.Set;

public class ScreenTrackingService extends AccessibilityService {
    private static final String TAG = "ScreenTrackingService PP";
    private static final long BREAK_LIMIT_MS = 30 * 60 * 1000; // 30 minutes

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
        Log.d(TAG, "Identified " + excludedPackages.size() + " excluded/launcher packages.");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "Accessibility Service Connected. Starting Foreground.");
        startForeground(AppConstants.NOTIFY_ID_SCREEN_TIME, 
                ScreenTimeNotificationHelper.getServiceNotification(this));
        
        // Reliability Fix: Ensure the sync worker is scheduled whenever the service starts/reconnects
        ScreenTimeScheduler.schedule(this, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            CharSequence pkg = event.getPackageName();
            if (pkg == null) return;
            
            String newPackage = pkg.toString();
            handleAppSwitch(newPackage);
        }
    }

    private void handleAppSwitch(String newPackage) {
        Log.v(TAG, "Window Changed: " + newPackage);
        long now = System.currentTimeMillis();

        if (isLockScreen(newPackage)) {
            Log.d(TAG, "Lock Screen detected. Resetting session.");
            cancelBreakAlarm();
            prefManager.putLong(AppConstants.KEY_SCREEN_SESSION_START, 0);
            
            // RELIABILITY: Since we know the user just finished a session (locked phone),
            // trigger an immediate sync to capture the usage accurately before the device potentially dozes.
            ScreenTimeScheduler.runNow(this, "LOCK_TRIGGER");
        } else {
            long sessionStartTime = prefManager.getLong(AppConstants.KEY_SCREEN_SESSION_START, 0);
            if (sessionStartTime == 0) {
                // Session not running. Check if we should START one.
                if (!isExcludedPackage(newPackage)) {
                    prefManager.putLong(AppConstants.KEY_SCREEN_SESSION_START, now);
                    scheduleBreakAlarm(BREAK_LIMIT_MS);
                    Log.d(TAG, "New Wellness Session started with app: " + newPackage);
                }
            } else {
                // Session already running. 
                // We DON'T reset if the user goes to home screen (launcher), 
                // so we just log that we are continuing.
                Log.d(TAG, "Continuing existing session across apps. Current: " + newPackage);
            }
        }
    }

    private boolean isLockScreen(String packageName) {
        return packageName.equals("com.android.systemui") || packageName.contains("keyguard");
    }

    private boolean isExcludedPackage(String packageName) {
        return excludedPackages.contains(packageName) || packageName.contains("launcher");
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
            // Using setAlarmClock is the absolute most reliable way to wake up from Doze
            AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent);
            alarmManager.setAlarmClock(info, pendingIntent);
            Log.d(TAG, "Break alarm (AlarmClock) scheduled in " + (delayMs / 60000) + "m");
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
            Log.d(TAG, "Break alarm canceled.");
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service Interrupted");
    }
}
