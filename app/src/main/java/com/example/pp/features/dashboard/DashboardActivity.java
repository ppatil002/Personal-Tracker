package com.example.pp.features.dashboard;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.pp.R;
import com.example.pp.data.local.model.NutritionSummary;
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
    private TextView tvRemainingCals, tvCarbsStats, tvProteinStats, tvFatsStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        initViews();
        setupBottomNav();
        observeViewModel();
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

        findViewById(R.id.btnSmallWater).setOnClickListener(v -> viewModel.addWater(100, System.currentTimeMillis()));
        findViewById(R.id.btnMedWater).setOnClickListener(v -> viewModel.addWater(250, System.currentTimeMillis()));
        findViewById(R.id.btnCustomWater).setOnClickListener(v -> showCustomWaterDialog());
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
            tvWaterCount.setText(current + " INTKS TODAY");
        });

        viewModel.getLastWaterIntake().observe(this, intake -> {
            if (intake != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String time = sdf.format(new Date(intake.timestamp));
                long diff = (System.currentTimeMillis() - intake.timestamp) / (60 * 1000);
                tvLastIntake.setText(time + " (" + diff + "m ago)");
            } else {
                tvLastIntake.setText("No intake yet");
            }
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

    private void showCustomWaterDialog() {
        String[] options = {"100", "150", "200", "250", "300", "500", "1000", "Pick Time & Amount"};
        new AlertDialog.Builder(this)
                .setTitle("Select Amount (ml)")
                .setItems(options, (dialog, which) -> {
                    if (which == options.length - 1) {
                        showTimeAndAmountPicker();
                    } else {
                        viewModel.addWater(Integer.parseInt(options[which]), System.currentTimeMillis());
                    }
                })
                .show();
    }

    private void showTimeAndAmountPicker() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            Calendar selectedTime = Calendar.getInstance();
            selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            selectedTime.set(Calendar.MINUTE, minute);
            
            // Now ask for amount
            String[] amounts = {"100", "250", "500", "750", "1000"};
            new AlertDialog.Builder(this)
                    .setTitle("Select Amount (ml)")
                    .setItems(amounts, (dialog, which) -> {
                        viewModel.addWater(Integer.parseInt(amounts[which]), selectedTime.getTimeInMillis());
                    })
                    .show();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
    }
}
