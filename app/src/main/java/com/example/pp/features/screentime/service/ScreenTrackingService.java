package com.example.pp.features.screentime.service;

import android.accessibilityservice.AccessibilityService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.example.pp.PPApplication;
import com.example.pp.core.orchestrator.screentime.ScreenTimeReceiver;
import com.example.pp.data.repository.ScreenTimeRepository;

public class ScreenTrackingService extends AccessibilityService {
    private static final String TAG = "ScreenTrackingService PP";
    private static final long BREAK_LIMIT_MS = 30 * 60 * 1000; // 30 minutes

    private long sessionStartTime = 0;
    private long lastAppStartTime = 0;
    private String lastPackageName = "";
    private AlarmManager alarmManager;
    private PendingIntent breakPendingIntent;
    private ScreenTimeRepository repository;

    @Override
    public void onCreate() {
        super.onCreate();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        repository = ((PPApplication) getApplication()).getScreenTimeRepository();
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

        // 1. Record usage for the PREVIOUS app
        if (!lastPackageName.isEmpty() && lastAppStartTime > 0) {
            if (!isExcludedPackage(lastPackageName)) {
                repository.insertSession(lastPackageName, lastAppStartTime, now);
            }
        }

        // 2. Handle Continuous Session for Break Reminders
        if (isExcludedPackage(newPackage)) {
            Log.d(TAG, "Excluded package detected (Lock/Home). Ending continuous session.");
            cancelBreakAlarm();
            sessionStartTime = 0;
        } else {
            if (sessionStartTime == 0) {
                sessionStartTime = now;
                scheduleBreakAlarm(BREAK_LIMIT_MS);
                Log.d(TAG, "New Wellness Session started.");
            } else {
                Log.d(TAG, "Continuing existing session across apps.");
            }
        }

        // 3. Update state for the NEW app
        lastPackageName = newPackage;
        lastAppStartTime = now;
    }

    private boolean isExcludedPackage(String packageName) {
        return packageName.equals(getPackageName()) || 
               packageName.contains("android.launcher") || 
               packageName.equals("com.android.systemui") ||
               packageName.contains("keyguard");
    }

    private void scheduleBreakAlarm(long delayMs) {
        Intent intent = new Intent(this, ScreenTimeReceiver.class);
        breakPendingIntent = PendingIntent.getBroadcast(
                this, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerAtMillis = System.currentTimeMillis() + delayMs;
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, breakPendingIntent);
            Log.d(TAG, "Break alarm scheduled in " + (delayMs / 60000) + "m");
        }
    }

    private void cancelBreakAlarm() {
        if (breakPendingIntent != null && alarmManager != null) {
            alarmManager.cancel(breakPendingIntent);
            breakPendingIntent = null;
            Log.d(TAG, "Break alarm canceled.");
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "Service Interrupted");
    }
}
