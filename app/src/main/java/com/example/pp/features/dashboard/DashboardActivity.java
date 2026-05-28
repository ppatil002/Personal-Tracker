package com.example.pp.features.dashboard;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.pp.R;
import com.example.pp.features.analytics.AnalyticsActivity;
import com.example.pp.features.settings.MoreActivity;
import com.example.pp.features.tracker.edit.EditTrackerActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {
    private DashboardViewModel viewModel;
    private WaveView waveView;
    private TextView tvWelcomeName, tvWaterPercent, tvWaterTarget, tvLastIntake, tvWaterCount;
    private CircularProgressIndicator calorieProgress;
    private LinearProgressIndicator carbsProgress, proteinProgress, fatsProgress;
    private TextView tvRemainingCals, tvCarbsStats, tvProteinStats, tvFatsStats, tvLastNutritionIntake;

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

        findViewById(R.id.btnSmallWater).setOnClickListener(v -> viewModel.addWater(100, System.currentTimeMillis()));
        findViewById(R.id.btnMedWater).setOnClickListener(v -> viewModel.addWater(250, System.currentTimeMillis()));
        findViewById(R.id.btnCustomWater).setOnClickListener(v -> showCustomIntakeDialog());
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
