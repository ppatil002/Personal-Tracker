package com.example.pp.features.dashboard;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.pp.R;
import com.example.pp.core.orchestrator.sleep.SleepScheduler;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.features.analytics.AnalyticsActivity;
import com.example.pp.features.screentime.analytics.ScreenTimeAnalyticsActivity;
import com.example.pp.features.settings.MoreActivity;
import com.example.pp.features.sleep.analytics.SleepAnalyticsActivity;
import com.example.pp.features.tracker.edit.EditTrackerActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DashboardActivity extends AppCompatActivity {
    private DashboardViewModel viewModel;
    private WaveView waveView;
    private TextView tvWelcomeName, tvWaterPercent, tvWaterTarget, tvLastIntake, tvWaterCount;
    private CircularProgressIndicator calorieProgress;
    private LinearProgressIndicator carbsProgress, proteinProgress, fatsProgress, sleepProgress;
    private TextView tvRemainingCals, tvCarbsStats, tvProteinStats, tvFatsStats, tvLastNutritionIntake;
    private TextView tvRemainingTime, tvUsedToday, tvCountdown, tvUntil, tvPotential, tvLastNight;
    private LinearLayout topAppsContainer;

    private Long lastWaterTimestamp = null;
    private Long lastNutritionTimestamp = null;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            updateLastIntakeStrings();
            refreshHandler.postDelayed(this, 60000); // Update every minute
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        initViews();
        setupBottomNav();
        observeViewModel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void initViews() {
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        waveView = findViewById(R.id.waveView);
        tvWaterPercent = findViewById(R.id.tvWaterPercent);
        tvWaterTarget = findViewById(R.id.tvWaterTarget);
        tvLastIntake = findViewById(R.id.tvLastIntake);
        tvWaterCount = findViewById(R.id.tvWaterCount);

        calorieProgress = findViewById(R.id.calorieProgress);
        tvRemainingCals = findViewById(R.id.tvRemainingCals);
        carbsProgress = findViewById(R.id.carbsProgress);
        proteinProgress = findViewById(R.id.proteinProgress);
        fatsProgress = findViewById(R.id.fatsProgress);
        tvCarbsStats = findViewById(R.id.tvCarbsStats);
        tvProteinStats = findViewById(R.id.tvProteinStats);
        tvFatsStats = findViewById(R.id.tvFatsStats);
        tvLastNutritionIntake = findViewById(R.id.tvLastNutritionIntake);
        
        tvRemainingTime = findViewById(R.id.tvRemainingTime);
        tvUsedToday = findViewById(R.id.tvUsedToday);
        topAppsContainer = findViewById(R.id.topAppsContainer);

        tvCountdown = findViewById(R.id.tvCountdown);
        tvUntil = findViewById(R.id.tvUntil);
        tvPotential = findViewById(R.id.tvPotential);
        tvLastNight = findViewById(R.id.tvLastNight);
        sleepProgress = findViewById(R.id.sleepProgress);

        findViewById(R.id.btnSmallWater).setOnClickListener(v -> viewModel.addWater(100, System.currentTimeMillis()));
        findViewById(R.id.btnMedWater).setOnClickListener(v -> viewModel.addWater(250, System.currentTimeMillis()));
        findViewById(R.id.btnCustomWater).setOnClickListener(v -> showCustomIntakeDialog());
        
        findViewById(R.id.ivScreenTimeIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, ScreenTimeAnalyticsActivity.class));
        });

        findViewById(R.id.ivSleepIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, SleepAnalyticsActivity.class));
        });

        findViewById(R.id.cardSleep).setOnClickListener(v -> showCustomWakeTimeDialog());
        SleepScheduler.reschedule(this);
    }

    private void setupBottomNav() {
        findViewById(R.id.navAnalytics).setOnClickListener(v -> startActivity(new Intent(this, AnalyticsActivity.class)));
        findViewById(R.id.btnCenterFab).setOnClickListener(v -> startActivity(new Intent(this, EditTrackerActivity.class)));
        findViewById(R.id.navMore).setOnClickListener(v -> startActivity(new Intent(this, MoreActivity.class)));
    }

    private void observeViewModel() {
        viewModel.getUserName().observe(this, name -> tvWelcomeName.setText("Hii, " + name + "!"));

        viewModel.getTodayWaterTotal().observe(this, total -> {
            int current = total != null ? total : 0;
            int target = 3000;
            float percent = (float) current / target;
            waveView.setProgress(percent);
            tvWaterPercent.setText(Math.round(percent * 100) + "%");
            tvWaterTarget.setText(current + "/" + target + " ml");
        });

        viewModel.getTodayWaterCount().observe(this, count -> {
            int current = count != null ? count : 0;
            tvWaterCount.setText(current + " INTAKES TODAY");
        });

        viewModel.getLastWaterIntake().observe(this, intake -> {
            lastWaterTimestamp = (intake != null) ? intake.timestamp : null;
            updateLastIntakeStrings();
        });

        viewModel.getLastNutritionIntake().observe(this, intake -> {
            lastNutritionTimestamp = (intake != null) ? intake.timestamp : null;
            updateLastIntakeStrings();
        });

        viewModel.getTodayNutritionSummary().observe(this, summary -> {
            double cals = summary != null && summary.totalCalories != null ? summary.totalCalories : 0.0;
            double carbs = summary != null && summary.totalCarbs != null ? summary.totalCarbs : 0.0;
            double protein = summary != null && summary.totalProtein != null ? summary.totalProtein : 0.0;
            double fats = summary != null && summary.totalFats != null ? summary.totalFats : 0.0;

            double targetCals = 2000.0;
            double targetCarbs = 200.0;
            double targetProtein = 150.0;
            double targetFats = 65.0;

            tvRemainingCals.setText(String.format(Locale.getDefault(), "%.1f kcal", Math.max(0, targetCals - cals)));
            calorieProgress.setProgress((int) Math.min(cals * 100 / targetCals, 100));

            tvCarbsStats.setText(String.format(Locale.getDefault(), "%.1f/%.0fg", carbs, targetCarbs));
            carbsProgress.setProgress((int) Math.min(carbs * 100 / targetCarbs, 100));

            tvProteinStats.setText(String.format(Locale.getDefault(), "%.1f/%.0fg", protein, targetProtein));
            proteinProgress.setProgress((int) Math.min(protein * 100 / targetProtein, 100));

            tvFatsStats.setText(String.format(Locale.getDefault(), "%.1f/%.0fg", fats, targetFats));
            fatsProgress.setProgress((int) Math.min(fats * 100 / targetFats, 100));
        });

        viewModel.getTodayScreenTime().observe(this, apps -> {
            if (apps == null || apps.isEmpty()) {
                android.util.Log.d("DashboardActivity", "No Screen Time apps observed from ViewModel.");
                tvUsedToday.setText("USED TODAY: 0m");
                tvRemainingTime.setText("4h 0m");
                topAppsContainer.removeAllViews();
                return;
            }
            android.util.Log.d("DashboardActivity", "Observed " + apps.size() + " apps with screen time.");
            long totalUsedMillis = 0;
            for (ScreenTimeDao.AppUsageSummary app : apps) {
                totalUsedMillis += app.totalDuration;
            }
            android.util.Log.d("DashboardActivity PP", "Total Used Millis: " + totalUsedMillis);

            long totalTargetMillis = 4 * 60 * 60 * 1000; // 4 hours
            long diffMillis = totalTargetMillis - totalUsedMillis;

            if (diffMillis < 0) {
                tvRemainingTime.setText("-" + formatMillis(Math.abs(diffMillis)));
                tvRemainingTime.setTextColor(getColor(R.color.error_red));
            } else {
                tvRemainingTime.setText(formatMillis(diffMillis));
                if (diffMillis <= 30 * 60 * 1000) {
                    tvRemainingTime.setTextColor(getColor(R.color.warning_orange));
                } else {
                    tvRemainingTime.setTextColor(getColor(R.color.white));
                }
            }
            tvUsedToday.setText("USED TODAY: " + formatMillis(totalUsedMillis));

            // Show top 3 apps
            topAppsContainer.removeAllViews();
            for (int i = 0; i < Math.min(3, apps.size()); i++) {
                ScreenTimeDao.AppUsageSummary app = apps.get(i);
                addAppUsageView(app);
            }
        });

        viewModel.getLastSleep().observe(this, this::updateSleepUI);
    }

    private void updateSleepUI(SleepEntity sleep) {
        if (sleep == null) {
            tvUntil.setText("UNTIL --");
            tvCountdown.setText("--");
            tvLastNight.setText("LAST NIGHT: --");
            tvPotential.setText("--");
            sleepProgress.setProgress(0);
            return;
        }

        long now = System.currentTimeMillis();
        long targetWakeTime = sleep.targetWakeTime;
        long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(8);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvUntil.setText("UNTIL " + timeFormat.format(new Date(bedtime)));

        if (sleep.startTime != 0 && sleep.endTime != 0) {
            // Last sleep session finished
            long duration = sleep.endTime - sleep.startTime;
            tvLastNight.setText("LAST NIGHT: " + formatMillis(duration).toUpperCase());
            
            // Percentage based on 8h target
            int percent = (int) Math.min(duration * 100 / TimeUnit.HOURS.toMillis(8), 100);
            sleepProgress.setProgress(percent);
            updateSleepProgressColor(percent);
            tvPotential.setText(percent + "% COMPLETED");
        } else if (sleep.startTime != 0 && sleep.endTime == 0) {
            // Sleeping
            tvCountdown.setText("SLEEPING...");
            long potentialDuration = targetWakeTime - now;
            int potentialPercent = (int) Math.max(0, Math.min(potentialDuration * 100 / TimeUnit.HOURS.toMillis(8), 100));
            sleepProgress.setProgress(potentialPercent);
            updateSleepProgressColor(potentialPercent);
            tvPotential.setText(potentialPercent + "% POTENTIAL");
            tvLastNight.setText("LAST NIGHT: --");
        } else {
            // Pending (startTime == 0) or waiting for bedtime
            tvLastNight.setText("LAST NIGHT: --");
            if (now < bedtime) {
                tvCountdown.setText(formatMillis(bedtime - now));
                tvPotential.setText("100% POTENTIAL");
                sleepProgress.setProgress(100);
                updateSleepProgressColor(100);
            } else if (now < targetWakeTime) {
                tvCountdown.setText("GET TO BED!");
                long potentialDuration = targetWakeTime - now;
                int potentialPercent = (int) Math.max(0, Math.min(potentialDuration * 100 / TimeUnit.HOURS.toMillis(8), 100));
                sleepProgress.setProgress(potentialPercent);
                updateSleepProgressColor(potentialPercent);
                tvPotential.setText(potentialPercent + "% POTENTIAL");
            } else {
                tvCountdown.setText("TIME TO WAKE!");
                tvPotential.setText("0% POTENTIAL");
                sleepProgress.setProgress(0);
                updateSleepProgressColor(0);
            }
        }
    }

    private void updateSleepProgressColor(int percent) {
        if (percent < 80) {
            sleepProgress.setIndicatorColor(getColor(R.color.error_red));
        } else if (percent < 90) {
            sleepProgress.setIndicatorColor(getColor(R.color.warning_orange));
        } else {
            sleepProgress.setIndicatorColor(getColor(R.color.accent_purple));
        }
    }

    private void showCustomWakeTimeDialog() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 6);
        cal.set(Calendar.MINUTE, 0);

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selected.set(Calendar.MINUTE, minute);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            
            if (selected.getTimeInMillis() <= System.currentTimeMillis()) {
                selected.add(Calendar.DAY_OF_YEAR, 1);
            }
            
            viewModel.updateTargetWakeTime(selected.getTimeInMillis());
            SleepScheduler.reschedule(this);
            Toast.makeText(this, "Wake up time updated to " + sdf.format(selected.getTime()), android.widget.Toast.LENGTH_SHORT).show();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private void addAppUsageView(ScreenTimeDao.AppUsageSummary app) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_dashboard_app_usage, topAppsContainer, false);
        TextView tvAppName = view.findViewById(R.id.tvAppName);
        TextView tvAppUsage = view.findViewById(R.id.tvAppUsage);
        ImageView ivAppIcon = view.findViewById(R.id.ivAppIcon);

        PackageManager pm = getPackageManager();
        String name = app.packageName;
        try {
            android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(app.packageName, 0);
            name = pm.getApplicationLabel(ai).toString();
            ivAppIcon.setImageDrawable(ai.loadIcon(pm));
        } catch (PackageManager.NameNotFoundException e) {
            ivAppIcon.setImageResource(R.drawable.ic_placeholder);
        }

        tvAppName.setText(name);
        tvAppUsage.setText(formatMillis(app.totalDuration));
        topAppsContainer.addView(view);
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private String formatMillis(long millis) {
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else {
            return minutes + "m";
        }
    }

    private void updateLastIntakeStrings() {
        tvLastIntake.setText(formatDuration(lastWaterTimestamp));
        tvLastNutritionIntake.setText(formatDuration(lastNutritionTimestamp));
    }

    private String formatDuration(Long timestamp) {
        if (timestamp == null) return "--";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String time = sdf.format(new Date(timestamp));
        long diffMinutes = (System.currentTimeMillis() - timestamp) / (60 * 1000);
        if (diffMinutes < 1) return time + " (Just now)";
        if (diffMinutes < 60) return time + " (" + diffMinutes + "m ago)";
        long hours = diffMinutes / 60;
        return time + " (" + hours + "h ago)";
    }

    private void showCustomIntakeDialog() {
        Calendar selectedTime = Calendar.getInstance();
        String[] amounts = {"100", "150", "200", "250", "300", "500", "750", "1000"};

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_intake, null);
        TextView tvTime = dialogView.findViewById(R.id.tvSelectedTime);
        ListView lvAmounts = dialogView.findViewById(R.id.lvAmounts);

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvTime.setText(sdf.format(selectedTime.getTime()));

        tvTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedTime.set(Calendar.MINUTE, minute);
                tvTime.setText(sdf.format(selectedTime.getTime()));
            }, selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE), false).show();
        });

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setTitle("Log Water Intake")
                .setNegativeButton("Cancel", null)
                .create();

        lvAmounts.setAdapter(new ArrayAdapter<>(this, R.layout.item_amount_selector, amounts));
        lvAmounts.setOnItemClickListener((parent, view, position, id) -> {
            viewModel.addWater(Integer.parseInt(amounts[position]), selectedTime.getTimeInMillis());
            dialog.dismiss();
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
        }
    }
}
