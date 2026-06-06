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
import android.os.PowerManager;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.remote.GoogleDriveClient;
import com.example.pp.data.repository.ScreenTimeRepository;
import com.example.pp.features.dashboard.DashboardActivity;
import com.example.pp.utils.DateUtils;

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
        ((PPApplication) getApplication()).getTaskRepository().seedTasksIfEmpty();

        // Ensure all seeding tasks (which run on diskIO) are completed before proceeding
        AppExecutors.getInstance().diskIO().execute(() -> {
            seedScreenTimeIfEmpty();
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

    private void seedScreenTimeIfEmpty() {
        ScreenTimeRepository repo = ((PPApplication) getApplication()).getScreenTimeRepository();
        AppDatabase db = AppDatabase.getInstance(this);

        int count = db.screenTimeDao().getCount();
        if (count == 0) {
            long now = System.currentTimeMillis();
            long end = DateUtils.getStartOfDay(now);
            long start = end - (30L * 24 * 60 * 60 * 1000); // 30 days ago

            Log.i("SplashActivity", "Seeding 30 days of screen time data...");
            repo.syncScreenTimeSync(start, end);
        }
    }

    private boolean checkAllCriticalPermissions() {
        boolean notifications = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        boolean alarms = am != null && am.canScheduleExactAlarms();
        boolean googleDrive = GoogleDriveClient.getInstance(this).isAuthorized();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean batteryIgnored = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());

        return notifications && alarms && googleDrive && batteryIgnored;
    }
}
