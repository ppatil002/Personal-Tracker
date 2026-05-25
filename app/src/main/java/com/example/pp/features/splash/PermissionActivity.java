package com.example.pp.features.splash;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.pp.R;
import com.example.pp.features.tracker.edit.EditTrackerActivity;

public class PermissionActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_NOTIFICATION = 101;
    private Button btnNotification, btnAlarm, btnBattery, btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        btnNotification = findViewById(R.id.btnNotificationPermission);
        btnAlarm = findViewById(R.id.btnAlarmPermission);
        btnBattery = findViewById(R.id.btnBatteryOptimization);
        btnContinue = findViewById(R.id.btnContinue);

        btnNotification.setOnClickListener(v -> requestNotificationPermission());
        btnAlarm.setOnClickListener(v -> requestAlarmPermission());
        btnBattery.setOnClickListener(v -> requestIgnoreBatteryOptimization());
        
        btnContinue.setOnClickListener(v -> {
            if (checkAllCriticalPermissions()) {
                startActivity(new Intent(this, EditTrackerActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Please grant critical permissions to proceed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionUI();
    }

    private void updatePermissionUI() {
        boolean notificationsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        btnNotification.setText(notificationsGranted ? "Notification Permission: GRANTED" : "Grant Notification Permission");
        btnNotification.setEnabled(!notificationsGranted);
        btnNotification.setAlpha(notificationsGranted ? 0.6f : 1.0f);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        boolean alarmsGranted = am != null && am.canScheduleExactAlarms();
        btnAlarm.setText(alarmsGranted ? "Exact Alarms: ALLOWED" : "Allow Exact Alarms");
        btnAlarm.setEnabled(!alarmsGranted);
        btnAlarm.setAlpha(alarmsGranted ? 0.6f : 1.0f);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean batteryIgnored = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        btnBattery.setText(batteryIgnored ? "Battery Optimization: IGNORED" : "Ignore Battery Optimization");
        btnBattery.setEnabled(!batteryIgnored);
        btnBattery.setAlpha(batteryIgnored ? 0.6f : 1.0f);
    }

    private boolean checkAllCriticalPermissions() {
        boolean notifications = true;
        notifications = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        boolean alarms = true;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarms = am != null && am.canScheduleExactAlarms();

        return notifications && alarms;
    }

    private void requestNotificationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATION);
    }

    private void requestAlarmPermission() {
        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void requestIgnoreBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Battery optimization already ignored.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
