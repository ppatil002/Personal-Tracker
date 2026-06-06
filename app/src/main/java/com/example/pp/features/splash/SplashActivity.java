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
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.remote.GoogleDriveClient;
import com.example.pp.features.dashboard.DashboardActivity;

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

//        ((PPApplication) getApplication()).getCategoryRepository().seedDatabaseIfEmpty();
//        ((PPApplication) getApplication()).getTagRepository().seedTagsIfEmpty();
//        ((PPApplication) getApplication()).getTransactionRepository().seedTransactionsIfEmpty();
        ((PPApplication) getApplication()).getSleepRepository().seedSleep();
//        seedScreenTime();
//        ((PPApplication) getApplication()).getTaskRepository().seedTasksIfEmpty();

        // Ensure all seeding tasks (which run on diskIO) are completed before proceeding
        AppExecutors.getInstance().diskIO().execute(() -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (checkAllCriticalPermissions()) {
                    startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, PermissionActivity.class));
                }
                finish();
            }, 2000); // 2 seconds delay
        });
    }

    private void seedScreenTime() {
        long start = com.example.pp.utils.DateUtils.getStartOfMonth(System.currentTimeMillis());
        long end = System.currentTimeMillis();
        ((PPApplication) getApplication()).getScreenTimeRepository().syncScreenTime(start, end);
    }

    private boolean checkAllCriticalPermissions() {
        boolean notifications = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        boolean alarms = am != null && am.canScheduleExactAlarms();
        boolean googleDrive = GoogleDriveClient.getInstance(this).isAuthorized();

        return notifications && alarms && googleDrive;
    }
}
