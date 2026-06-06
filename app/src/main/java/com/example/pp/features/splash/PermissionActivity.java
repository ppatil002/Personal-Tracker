package com.example.pp.features.splash;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.pp.R;
import com.example.pp.data.remote.GoogleDriveClient;
import com.example.pp.features.dashboard.DashboardActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class PermissionActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_NOTIFICATION = 101;
    private static final int REQUEST_CODE_GOOGLE_SIGN_IN = 102;
    private Button btnNotification, btnAlarm, btnBattery, btnGoogleDrive, btnUsageStats, btnAccessibility, btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        btnNotification = findViewById(R.id.btnNotificationPermission);
        btnAlarm = findViewById(R.id.btnAlarmPermission);
        btnBattery = findViewById(R.id.btnBatteryOptimization);
        btnGoogleDrive = findViewById(R.id.btnGoogleDrive);
        btnUsageStats = findViewById(R.id.btnUsageStats);
        btnAccessibility = findViewById(R.id.btnAccessibility);
        btnContinue = findViewById(R.id.btnContinue);

        btnNotification.setOnClickListener(v -> requestNotificationPermission());
        btnAlarm.setOnClickListener(v -> requestAlarmPermission());
        btnBattery.setOnClickListener(v -> requestIgnoreBatteryOptimization());
        btnGoogleDrive.setOnClickListener(v -> requestGoogleDriveAccess());
        btnUsageStats.setOnClickListener(v -> requestUsageStatsPermission());
        btnAccessibility.setOnClickListener(v -> requestAccessibilityPermission());
        
        btnContinue.setOnClickListener(v -> {
            if (checkAllCriticalPermissions()) {
                startActivity(new Intent(this, DashboardActivity.class));
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

        boolean googleDriveAuthorized = GoogleDriveClient.getInstance(this).isAuthorized();
        btnGoogleDrive.setText(googleDriveAuthorized ? "Google Drive: CONNECTED" : "Connect Google Drive");
        btnGoogleDrive.setEnabled(!googleDriveAuthorized);
        btnGoogleDrive.setAlpha(googleDriveAuthorized ? 0.6f : 1.0f);

        boolean usageStatsGranted = hasUsageStatsPermission();
        btnUsageStats.setText(usageStatsGranted ? "Usage Stats: GRANTED" : "Grant Usage Stats Access");
        btnUsageStats.setEnabled(!usageStatsGranted);
        btnUsageStats.setAlpha(usageStatsGranted ? 0.6f : 1.0f);

        boolean accessibilityGranted = isAccessibilityServiceEnabled();
        btnAccessibility.setText(accessibilityGranted ? "Accessibility Service: ENABLED" : "Enable Accessibility Service");
        btnAccessibility.setEnabled(!accessibilityGranted);
        btnAccessibility.setAlpha(accessibilityGranted ? 0.6f : 1.0f);
    }

    private boolean checkAllCriticalPermissions() {
        boolean notifications = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        boolean alarms = am != null && am.canScheduleExactAlarms();
        boolean googleDrive = GoogleDriveClient.getInstance(this).isAuthorized();
        boolean usageStats = hasUsageStatsPermission();
        boolean accessibility = isAccessibilityServiceEnabled();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean batteryIgnored = pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());

        return notifications && alarms && googleDrive && usageStats && accessibility && batteryIgnored;
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(android.content.Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + com.example.pp.features.screentime.service.ScreenTrackingService.class.getName();
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException ignored) {}

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                String[] services = settingValue.split(":");
                for (String s : services) {
                    if (s.equalsIgnoreCase(service)) return true;
                }
            }
        }
        return false;
    }

    private void requestUsageStatsPermission() {
        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    private void requestAccessibilityPermission() {
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
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
        }
    }

    private void requestGoogleDriveAccess() {
        startActivityForResult(GoogleDriveClient.getFullGoogleSignInClient(this).getSignInIntent(), REQUEST_CODE_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                task.getResult(ApiException.class);
                GoogleDriveClient.getInstance(this).refresh(this);
                updatePermissionUI();
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updatePermissionUI();
            }
        }
    }
}
