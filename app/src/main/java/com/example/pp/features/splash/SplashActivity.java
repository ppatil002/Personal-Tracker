package com.example.pp.features.splash;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.features.tracker.edit.EditTrackerActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Clear all notifications and reset badge
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        ((PPApplication) getApplication()).getCategoryRepository().seedDatabaseIfEmpty();
        ((PPApplication) getApplication()).getTagRepository().seedTagsIfEmpty();
        ((PPApplication) getApplication()).getTransactionRepository().seedTransactionsIfEmpty();
        ((PPApplication) getApplication()).getTaskRepository().seedTasksIfEmpty();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (checkAllCriticalPermissions()) {
                startActivity(new Intent(SplashActivity.this, EditTrackerActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, PermissionActivity.class));
            }
            finish();
        }, 2000); // 2 seconds delay
    }

    private boolean checkAllCriticalPermissions() {
        boolean notifications = true;
        notifications = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        boolean alarms = true;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarms = am != null && am.canScheduleExactAlarms();

        return notifications && alarms;
    }
}
