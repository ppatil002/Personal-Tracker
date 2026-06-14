package com.example.pp.features.dashboard;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.lifecycle.ViewModelProvider;

import com.example.pp.R;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.orchestrator.sleep.SleepScheduler;
import com.example.pp.core.orchestrator.water.WaterIntakeScheduler;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.data.local.entity.NutrientEntity;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.remote.ApiNinjasService;
import com.example.pp.features.analytics.AnalyticsActivity;
import com.example.pp.features.hydration.analytics.WaterAnalyticsActivity;
import com.example.pp.features.nutrition.analytics.NutritionAnalyticsActivity;
import com.example.pp.features.quotes.SavedQuotesActivity;
import com.example.pp.features.screentime.analytics.ScreenTimeAnalyticsActivity;
import com.example.pp.features.settings.MoreActivity;
import com.example.pp.features.sleep.analytics.SleepAnalyticsActivity;
import com.example.pp.features.tracker.edit.EditTrackerActivity;
import com.example.pp.utils.DateUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DashboardActivity extends AppCompatActivity {
    private DashboardViewModel viewModel;
    private WaveView waveView;
    private TextView tvWelcomeName, tvWaterPercent, tvWaterTarget, tvLastIntake, tvWaterCount;
    private CircularProgressIndicator calorieProgress;
    private LinearProgressIndicator carbsProgress, proteinProgress, fatsProgress, sleepProgress;
    private TextView tvRemainingCals, tvCalorieStats, tvCarbsStats, tvProteinStats, tvFatsStats, tvLastNutritionIntake;
    private TextView tvRemainingTime, tvUsedToday, tvCountdown, tvUntil, tvPotential, tvLastNight;
    private TextView tvQuoteMain, tvQuoteAuthor, tvQuoteWork, tvHistoryText;
    private ImageButton btnSaveQuote;
    private com.google.android.material.chip.ChipGroup cgQuoteCategories;
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
        tvCalorieStats = findViewById(R.id.tvCalorieStats);
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

        tvQuoteMain = findViewById(R.id.tvQuoteMain);
        tvQuoteAuthor = findViewById(R.id.tvQuoteAuthor);
        tvQuoteWork = findViewById(R.id.tvQuoteWork);
        cgQuoteCategories = findViewById(R.id.cgQuoteCategories);
        tvHistoryText = findViewById(R.id.tvHistoryText);
        btnSaveQuote = findViewById(R.id.btnSaveQuote);

        findViewById(R.id.btnRefreshQuote).setOnClickListener(v -> viewModel.refreshQuote());

        btnSaveQuote.setOnClickListener(v -> showSaveQuoteDialog());

        findViewById(R.id.cardQuote).setOnClickListener(v -> {
            startActivity(new Intent(this, SavedQuotesActivity.class));
        });

        findViewById(R.id.btnSmallWater).setOnClickListener(v -> viewModel.addWater(100, System.currentTimeMillis()));
        findViewById(R.id.btnMedWater).setOnClickListener(v -> viewModel.addWater(250, System.currentTimeMillis()));
        findViewById(R.id.btnCustomWater).setOnClickListener(v -> showCustomIntakeDialog());
        
        findViewById(R.id.tvHydrationTitle).setOnClickListener(v -> {
            startActivity(new Intent(this, WaterAnalyticsActivity.class));
        });

        findViewById(R.id.tvNutritionTitle).setOnClickListener(v -> {
            startActivity(new Intent(this, NutritionAnalyticsActivity.class));
        });

        findViewById(R.id.ivScreenTimeIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, ScreenTimeAnalyticsActivity.class));
        });

        findViewById(R.id.ivSleepIcon).setOnClickListener(v -> {
            startActivity(new Intent(this, SleepAnalyticsActivity.class));
        });

        findViewById(R.id.cardSleep).setOnClickListener(v -> {
            viewModel.getLastSleepSync(this::showSleepEntryDialog);
        });

        findViewById(R.id.cardNutrition).setOnClickListener(v -> {
            showLogNutritionDialog();
        });

        findViewById(R.id.cardTodayHistory).setOnClickListener(v -> {
            if (tvHistoryText.getMaxLines() == 2) {
                tvHistoryText.setMaxLines(Integer.MAX_VALUE);
            } else {
                tvHistoryText.setMaxLines(2);
            }
        });

        // TODO Do I need to manually schedule the activity again and again.
        SleepScheduler.schedule(this, null);
        WaterIntakeScheduler.schedule(this, null);
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
            double target = AppConstants.TARGET_WATER_ML;
            float percent = (float) (current / target);
            waveView.setProgress(percent);
            tvWaterPercent.setText(Math.round(percent * 100) + "%");
            tvWaterTarget.setText(current + "/" + (int)target + " ml");
        });

        viewModel.getTodayWaterCount().observe(this, count -> {
            int current = count != null ? count : 0;
            tvWaterCount.setText(current + " INTAKES TODAY");
        });

        viewModel.getLastWaterIntakeForCurrentDate().observe(this, intake -> {
            lastWaterTimestamp = (intake != null) ? intake.timestamp : null;
            updateLastIntakeStrings();
        });

        viewModel.getLastNutritionIntake().observe(this, intake -> {
            lastNutritionTimestamp = (intake != null) ? intake.intakeTimestamp : null;
            updateLastIntakeStrings();
        });

        viewModel.getTodayNutritionSummary().observe(this, summary -> {
            double cals = summary != null && summary.totalCalories != null ? summary.totalCalories : 0.0;
            double carbs = summary != null && summary.totalCarbs != null ? summary.totalCarbs : 0.0;
            double protein = summary != null && summary.totalProtein != null ? summary.totalProtein : 0.0;
            double fats = summary != null && summary.totalFats != null ? summary.totalFats : 0.0;

            double targetCals = AppConstants.TARGET_CALORIES;
            double targetCarbs = AppConstants.TARGET_CARBS;
            double targetProtein = AppConstants.TARGET_PROTEIN;
            double targetFats = AppConstants.TARGET_FATS;

            tvRemainingCals.setText(String.format(Locale.getDefault(), "%.0f kcal", Math.max(0, targetCals - cals)));
            tvCalorieStats.setText(String.format(Locale.getDefault(), "%.0f / %.0f", cals, targetCals));
            calorieProgress.setProgress((int) Math.min(cals * 100 / targetCals, 100));

            double carbsPercent = (carbs * 100 / targetCarbs);
            tvCarbsStats.setText(String.format(Locale.getDefault(), "%.1f/%.0fg (%.1f%%)", carbs, targetCarbs, carbsPercent));
            carbsProgress.setProgress((int) Math.min(carbsPercent, 100));

            double proteinPercent = (protein * 100 / targetProtein);
            tvProteinStats.setText(String.format(Locale.getDefault(), "%.1f/%.0fg (%.1f%%)", protein, targetProtein, proteinPercent));
            proteinProgress.setProgress((int) Math.min(proteinPercent, 100));

            double fatsPercent = (fats * 100 / targetFats);
            tvFatsStats.setText(String.format(Locale.getDefault(), "%.1f/%.0fg (%.1f%%)", fats, targetFats, fatsPercent));
            fatsProgress.setProgress((int) Math.min(fatsPercent, 100));
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
                tvRemainingTime.setText("-" + DateUtils.convertTimestampIntoHoursAndMinutes(Math.abs(diffMillis)));
                tvRemainingTime.setTextColor(getColor(R.color.error_red));
            } else {
                tvRemainingTime.setText(DateUtils.convertTimestampIntoHoursAndMinutes(diffMillis));
                if (diffMillis <= 30 * 60 * 1000) {
                    tvRemainingTime.setTextColor(getColor(R.color.warning_orange));
                } else {
                    tvRemainingTime.setTextColor(getColor(R.color.white));
                }
            }
            tvUsedToday.setText("USED TODAY: " + DateUtils.convertTimestampIntoHoursAndMinutes(totalUsedMillis));

            // Show top 3 apps
            topAppsContainer.removeAllViews();
            for (int i = 0; i < Math.min(3, apps.size()); i++) {
                ScreenTimeDao.AppUsageSummary app = apps.get(i);
                addAppUsageView(app);
            }
        });

        viewModel.getLastSleep().observe(this, sleep -> {
            updateSleepUI(sleep);
            
            // Last day sleep duration.
            if (sleep != null) {
                long yesterday = DateUtils.getStartOfDay(sleep.date - 24 * 3600 * 1000L);
                viewModel.getSleepByDateSync(yesterday, prevSleep -> {
                    if (prevSleep != null && prevSleep.startTime != 0 && prevSleep.endTime != 0) {
                        String duration = DateUtils.convertTimestampIntoHoursAndMinutes(prevSleep.endTime - prevSleep.startTime).toUpperCase();
                        tvLastNight.setText("LAST NIGHT: " + duration);
                    }
                });
            }
        });

        viewModel.getCurrentQuote().observe(this, quote -> {
            if (quote != null) {
                tvQuoteMain.setText("“ " + quote.quote + " ”");
                tvQuoteAuthor.setText(quote.author);
                tvQuoteWork.setText(quote.work != null ? quote.work : "");
                
                cgQuoteCategories.removeAllViews();
                if (quote.categories != null) {
                    for (String cat : quote.categories) {
                        Chip chip = new Chip(new ContextThemeWrapper(this, R.style.QuotesCategoryChip), null, 0);
                        chip.setText(cat.toUpperCase());
                        cgQuoteCategories.addView(chip);
                    }
                }

                // Check if already saved
                viewModel.isQuoteSaved(quote.quote).observe(this, saved -> {
                    if (saved != null && saved) {
                        btnSaveQuote.setColorFilter(getColor(R.color.accent_purple));
                    } else {
                        btnSaveQuote.setColorFilter(getColor(R.color.text_muted));
                    }
                });
            }
        });

        viewModel.getCurrentHistory().observe(this, history -> {
            if (history != null) {
                tvHistoryText.setText(history.year + ": " + history.event);
            }
        });
    }

    private void showSaveQuoteDialog() {
        ApiNinjasService.QuoteResponse currentQuote = viewModel.getCurrentQuote().getValue();
        if (currentQuote == null) {
            Toast.makeText(this, "No quote to save", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_quote, null);
        TextView tvQuotePreview = dialogView.findViewById(R.id.tvQuotePreview);
        ChipGroup cgSelectedCategories = dialogView.findViewById(R.id.cgSelectedCategories);
        AutoCompleteTextView actvCategory = dialogView.findViewById(R.id.actvCategoryFilter);
        EditText etNotes = dialogView.findViewById(R.id.etQuoteNotes);

        tvQuotePreview.setText("“ " + currentQuote.quote + " ”\n\n— " + currentQuote.author);

        List<String> selectedCategories = new ArrayList<>();
        if (currentQuote.categories != null) {
            for (String cat : currentQuote.categories) {
                addCategoryChip(cat, cgSelectedCategories, selectedCategories);
            }
        }

        viewModel.getAvailableCategories().observe(this, categories -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown_divider, R.id.tvDropdownItem, categories);
            actvCategory.setAdapter(adapter);
            actvCategory.setOnClickListener(v -> actvCategory.showDropDown());
            actvCategory.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && actvCategory.getAdapter() != null) {
                    actvCategory.postDelayed(actvCategory::showDropDown, 200);
                }
            });
            actvCategory.setOnItemClickListener((parent, view, position, id) -> {
                String cat = (String) parent.getItemAtPosition(position);
                addCategoryChip(cat, cgSelectedCategories, selectedCategories);
                actvCategory.setText("");
            });
        });

        actvCategory.setOnEditorActionListener((v, actionId, event) -> {
            String cat = actvCategory.getText().toString().trim();
            if (!cat.isEmpty()) {
                addCategoryChip(cat, cgSelectedCategories, selectedCategories);
                actvCategory.setText("");
                return true;
            }
            return false;
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Save Quote")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    viewModel.isQuoteSaved(currentQuote.quote).observe(this, saved -> {
                        if (saved != null && saved) {
                            Toast.makeText(this, "Quote already saved!", Toast.LENGTH_SHORT).show();
                        } else {
                            String notes = etNotes.getText().toString().trim();
                            viewModel.saveQuote(currentQuote.quote, currentQuote.author, selectedCategories, notes);
                            Toast.makeText(this, "Quote saved!", Toast.LENGTH_SHORT).show();
                            btnSaveQuote.setColorFilter(getColor(R.color.accent_purple));
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
        }
    }

    private void addCategoryChip(String category, com.google.android.material.chip.ChipGroup chipGroup, List<String> selectedList) {
        String catClean = category.trim();
        if (catClean.isEmpty() || selectedList.contains(catClean)) return;

        selectedList.add(catClean);
        Chip chip = new Chip(new ContextThemeWrapper(this, R.style.QuotesCategoryChip), null, 0);
        chip.setText(catClean.toUpperCase());
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            chipGroup.removeView(chip);
            selectedList.remove(catClean);
        });
        chipGroup.addView(chip);
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
            tvLastNight.setText("LAST NIGHT: " + DateUtils.convertTimestampIntoHoursAndMinutes(duration).toUpperCase());
            tvCountdown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28); // Default size
            
            // Percentage based on 8h target
            int percent = (int) Math.min(duration * 100 / TimeUnit.HOURS.toMillis(8), 100);
            sleepProgress.setProgress(percent);
            updateSleepProgressColor(percent);
            tvPotential.setText(percent + "% COMPLETED");
        } else if (sleep.startTime != 0 && sleep.endTime == 0) {
            // Sleeping
            tvCountdown.setText("SLEEPING...");
            tvCountdown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24); // Reduced size
            long potentialDuration = targetWakeTime - now;
            int potentialPercent = (int) Math.max(0, Math.min(potentialDuration * 100 / TimeUnit.HOURS.toMillis(8), 100));
            sleepProgress.setProgress(potentialPercent);
            updateSleepProgressColor(potentialPercent);
            tvPotential.setText(potentialPercent + "% POTENTIAL");
            tvLastNight.setText("LAST NIGHT: --");
        } else {
            // Pending (startTime == 0) or waiting for bedtime
            tvCountdown.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28); // Default size
            tvLastNight.setText("LAST NIGHT: --");
            if (now < bedtime) {
                tvCountdown.setText(DateUtils.convertTimestampIntoHoursAndMinutes(bedtime - now));
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

    private void showSleepEntryDialog(SleepEntity initialSleep) {
        Calendar calTarget = Calendar.getInstance();
        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();
        Calendar calDate = Calendar.getInstance();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sleep_entry, null);
        TextView tvSleepDate = dialogView.findViewById(R.id.tvSleepDate);
        TextView tvTargetWake = dialogView.findViewById(R.id.tvTargetWakeTime);
        TextView tvStartTime = dialogView.findViewById(R.id.tvStartTime);
        TextView tvEndTime = dialogView.findViewById(R.id.tvEndTime);
        android.widget.EditText etDelayReason = dialogView.findViewById(R.id.etDelayReason);

        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());

        DashboardViewModel.OnSleepLoadedCallback populateUI = sleep -> {
            if (sleep != null) {
                if (sleep.targetWakeTime != 0) {
                    calTarget.setTimeInMillis(sleep.targetWakeTime);
                    tvTargetWake.setText(sdfTime.format(calTarget.getTime()));
                } else {
                    calTarget.set(Calendar.HOUR_OF_DAY, 6);
                    calTarget.set(Calendar.MINUTE, 0);
                    tvTargetWake.setText("Not Set");
                }

                if (sleep.startTime != 0) {
                    calStart.setTimeInMillis(sleep.startTime);
                    tvStartTime.setText(sdfTime.format(calStart.getTime()));
                } else {
                    calStart.setTimeInMillis(0);
                    tvStartTime.setText("Not Set");
                }

                if (sleep.endTime != 0) {
                    calEnd.setTimeInMillis(sleep.endTime);
                    tvEndTime.setText(sdfTime.format(calEnd.getTime()));
                } else {
                    calEnd.setTimeInMillis(0);
                    tvEndTime.setText("Not Set");
                }

                if (sleep.date != 0) {
                    calDate.setTimeInMillis(sleep.date);
                    tvSleepDate.setText(sdfDate.format(calDate.getTime()));
                }
                
                etDelayReason.setText(sleep.getDelayReason() != null ? sleep.getDelayReason() : "");
            } else {
                tvTargetWake.setText("Not Set");
                tvStartTime.setText("Not Set");
                tvEndTime.setText("Not Set");
                calStart.setTimeInMillis(0);
                calEnd.setTimeInMillis(0);
                etDelayReason.setText("");
                // We keep the calDate as selected by the user, but reset times
                tvSleepDate.setText(sdfDate.format(calDate.getTime()));
            }
        };

        // First time population
        if (initialSleep != null && initialSleep.date != 0) {
            calDate.setTimeInMillis(initialSleep.date);
        } else {
            calDate.setTimeInMillis(DateUtils.getStartOfDay(System.currentTimeMillis()));
        }
        populateUI.onLoaded(initialSleep);

        tvSleepDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calDate.set(year, month, dayOfMonth);
                long selectedDate = DateUtils.getStartOfDay(calDate.getTimeInMillis());
                tvSleepDate.setText(sdfDate.format(calDate.getTime()));
                
                // Fetch details based on new date
                viewModel.getSleepByDateSync(selectedDate, populateUI);
            }, calDate.get(Calendar.YEAR), calDate.get(Calendar.MONTH), calDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvTargetWake.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calTarget.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calTarget.set(Calendar.MINUTE, minute);
                calTarget.set(Calendar.SECOND, 0);
                calTarget.set(Calendar.MILLISECOND, 0);
                tvTargetWake.setText(sdfTime.format(calTarget.getTime()));
            }, calTarget.get(Calendar.HOUR_OF_DAY), calTarget.get(Calendar.MINUTE), false).show();
        });

        tvStartTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calStart.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calStart.set(Calendar.MINUTE, minute);
                calStart.set(Calendar.SECOND, 0);
                calStart.set(Calendar.MILLISECOND, 0);
                tvStartTime.setText(sdfTime.format(calStart.getTime()));
            }, calStart.get(Calendar.HOUR_OF_DAY) == 0 ? 22 : calStart.get(Calendar.HOUR_OF_DAY), calStart.get(Calendar.MINUTE), false).show();
        });

        tvEndTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                calEnd.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calEnd.set(Calendar.MINUTE, minute);
                calEnd.set(Calendar.SECOND, 0);
                calEnd.set(Calendar.MILLISECOND, 0);
                tvEndTime.setText(sdfTime.format(calEnd.getTime()));
            }, calEnd.get(Calendar.HOUR_OF_DAY) == 0 ? 6 : calEnd.get(Calendar.HOUR_OF_DAY), calEnd.get(Calendar.MINUTE), false).show();
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Sleep Manual Entry")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    
                    
                    
                    long sleepDate = DateUtils.getStartOfDay(calDate.getTimeInMillis());
                    
                    // Target Wake Time and End Time are always +1 day from the selected Sleep Date
                    Calendar finalTarget = Calendar.getInstance();
                    finalTarget.setTimeInMillis(sleepDate);
                    finalTarget.add(Calendar.DAY_OF_YEAR, 1);
                    finalTarget.set(Calendar.HOUR_OF_DAY, calTarget.get(Calendar.HOUR_OF_DAY));
                    finalTarget.set(Calendar.MINUTE, calTarget.get(Calendar.MINUTE));
                    finalTarget.set(Calendar.SECOND, 0);
                    finalTarget.set(Calendar.MILLISECOND, 0);

                    long endTs = 0;
                    if (calEnd.getTimeInMillis() != 0) {
                        Calendar finalEnd = (Calendar) finalTarget.clone();
                        finalEnd.set(Calendar.HOUR_OF_DAY, calEnd.get(Calendar.HOUR_OF_DAY));
                        finalEnd.set(Calendar.MINUTE, calEnd.get(Calendar.MINUTE));
                        endTs = finalEnd.getTimeInMillis();
                    }

                    // Start Time logic: 
                    // If selected time >= 12 PM, it's on the Sleep Date (e.g., 11 PM on June 2)
                    // If selected time < 12 PM, it's on the Next Day (e.g., 1 AM on June 3)
                    long startTs = 0;
                    if (calStart.getTimeInMillis() != 0) {
                        Calendar finalStart = (Calendar) calDate.clone();
                        if (calStart.get(Calendar.HOUR_OF_DAY) < 12) {
                            finalStart.add(Calendar.DAY_OF_YEAR, 1);
                        }
                        finalStart.set(Calendar.HOUR_OF_DAY, calStart.get(Calendar.HOUR_OF_DAY));
                        finalStart.set(Calendar.MINUTE, calStart.get(Calendar.MINUTE));
                        finalStart.set(Calendar.SECOND, 0);
                        finalStart.set(Calendar.MILLISECOND, 0);
                        startTs = finalStart.getTimeInMillis();
                    }

                    long finalEndTs = endTs;
                    String reason = etDelayReason.getText().toString().trim();
                    viewModel.saveSleepEntry(startTs, endTs, finalTarget.getTimeInMillis(), sleepDate, reason);

                    // Requirement: Create next day entry if end time was just added
                    if (finalEndTs != 0) {
                        long nextSleepDate = DateUtils.getStartOfDay(finalEndTs);
                        // Using callback to ensure we check DB for existing
                        viewModel.getSleepByDateSync(nextSleepDate, nextSleep -> {
                            if (nextSleep == null) {
                                Calendar nextCal = Calendar.getInstance();
                                nextCal.setTimeInMillis(finalEndTs);
                                nextCal.add(Calendar.DAY_OF_YEAR, 1);
                                nextCal.set(Calendar.HOUR_OF_DAY, 6);
                                nextCal.set(Calendar.MINUTE, 0);
                                nextCal.set(Calendar.SECOND, 0);
                                nextCal.set(Calendar.MILLISECOND, 0);
                                viewModel.saveSleepEntry(0, 0, nextCal.getTimeInMillis(), nextSleepDate, "");
                            }
                        });
                    }

                    SleepScheduler.schedule(this, null);
                    Toast.makeText(this, "Sleep record updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
        }
    }

    private void showLogNutritionDialog() {
        Calendar selectedDateTime = Calendar.getInstance();
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_log_nutrition, null);
        TextView tvDate = dialogView.findViewById(R.id.tvLogDate);
        TextView tvTime = dialogView.findViewById(R.id.tvLogTime);
        AutoCompleteTextView actvFood = dialogView.findViewById(R.id.actvFoodItem);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        TextView tvUnitInfo = dialogView.findViewById(R.id.tvUnitInfo);
        AutoCompleteTextView actvTags = dialogView.findViewById(R.id.actvTags);
        TextView tvPreview = dialogView.findViewById(R.id.tvPreviewStats);

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        tvDate.setText(sdfDate.format(selectedDateTime.getTime()));
        tvTime.setText(sdfTime.format(selectedDateTime.getTime()));

        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDateTime.set(year, month, day);
                tvDate.setText(sdfDate.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hour, min) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour);
                selectedDateTime.set(Calendar.MINUTE, min);
                tvTime.setText(sdfTime.format(selectedDateTime.getTime()));
            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), false).show();
        });

        final NutrientEntity[] selectedNutrient = {null};

        String[] tagSuggestions = {"Pre workout", "Post Workout", "Morning Breakfast", "Lunch", "Evening Breakfast", "Dinner"};
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(this, R.layout.item_dropdown_divider, R.id.tvDropdownItem, tagSuggestions);
        actvTags.setAdapter(tagAdapter);
        actvTags.setOnClickListener(v -> actvTags.showDropDown());
        actvTags.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && actvTags.getAdapter() != null) {
                actvTags.postDelayed(actvTags::showDropDown, 200);
            }
        });

        etQuantity.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateNutritionPreview(s.toString(), selectedNutrient[0], tvPreview);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        viewModel.getAllNutrients(nutrients -> {
            List<String> names = new ArrayList<>();
            for (NutrientEntity n : nutrients) names.add(n.getFoodItem());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_dropdown_divider, R.id.tvDropdownItem, names);
            actvFood.setAdapter(adapter);

            actvFood.setOnClickListener(v -> actvFood.showDropDown());
            actvFood.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && actvFood.getAdapter() != null) {
                    actvFood.postDelayed(actvFood::showDropDown, 200);
                }
            });
            actvFood.setOnItemClickListener((parent, view, position, id) -> {
                String selectedName = (String) parent.getItemAtPosition(position);
                for (NutrientEntity n : nutrients) {
                    if (n.getFoodItem().equals(selectedName)) {
                        selectedNutrient[0] = n;
                        tvUnitInfo.setText("Unit: " + n.getUnit() + " (Base: " + n.getPerUnit() + " " + n.getUnit() + ")");
                        updateNutritionPreview(etQuantity.getText().toString(), selectedNutrient[0], tvPreview);
                        break;
                    }
                }
            });

            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Log Nutrition")
                    .setView(dialogView)
                    .setPositiveButton("Save", (d, which) -> {
                        String qtyStr = etQuantity.getText().toString();
                        String foodName = actvFood.getText().toString().trim();

                        if (foodName.isEmpty() || qtyStr.isEmpty()) {
                            Toast.makeText(this, "Please enter food and quantity", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (selectedNutrient[0] != null && selectedNutrient[0].getFoodItem().equalsIgnoreCase(foodName)) {
                            performSaveNutrition(selectedNutrient[0], qtyStr, selectedDateTime.getTimeInMillis(), actvTags.getText().toString());
                        } else {
                            // Check if food exists in the loaded list
                            NutrientEntity existing = null;
                            for (NutrientEntity n : nutrients) {
                                if (n.getFoodItem().equalsIgnoreCase(foodName)) {
                                    existing = n;
                                    break;
                                }
                            }

                            if (existing != null) {
                                performSaveNutrition(existing, qtyStr, selectedDateTime.getTimeInMillis(), actvTags.getText().toString());
                            } else {
                                // Show alert to add new nutrient
                                AlertDialog newNutrientDialog = new MaterialAlertDialogBuilder(this)
                                        .setTitle("New Food Item")
                                        .setMessage("'" + foodName + "' is not in your knowledge base. Would you like to add it?")
                                        .setPositiveButton("Yes", (di, w) -> {
                                            viewModel.addNewNutrient(foodName, newId -> {
                                                // After adding, we need to log it. We fetch all again to get the entity.
                                                viewModel.getAllNutrients(refreshed -> {
                                                    for (NutrientEntity rn : refreshed) {
                                                        if (rn.getNutrientId() == newId) {
                                                            performSaveNutrition(rn, qtyStr, selectedDateTime.getTimeInMillis(), actvTags.getText().toString());
                                                            break;
                                                        }
                                                    }
                                                });
                                            });
                                        })
                                        .setNegativeButton("No", null)
                                        .create();
                                
                                newNutrientDialog.show();
                                if (newNutrientDialog.getWindow() != null) {
                                    newNutrientDialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
                                }
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create();

            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.color.card_background);
            }
        });
    }

    private void performSaveNutrition(NutrientEntity nutrient, String qtyStr, long timestamp, String tags) {
        try {
            double quantity = Double.parseDouble(qtyStr);
            String tagStr = tags.trim();

            viewModel.addNutrition(
                    nutrient.getNutrientId(),
                    quantity,
                    timestamp,
                    tagStr.isEmpty() ? null : tagStr
            );
            Toast.makeText(this, "Nutrition logged", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNutritionPreview(String qtyStr, NutrientEntity nutrient, TextView tvPreview) {
        if (nutrient == null || qtyStr.isEmpty()) {
            tvPreview.setText("Calculated nutrients will appear here");
            return;
        }
        try {
            double qty = Double.parseDouble(qtyStr);
            double ratio = qty / nutrient.getPerUnit();
            double cals = nutrient.getCalories() * ratio;
            double pro = nutrient.getProtein() * ratio;
            double carb = nutrient.getCarbs() * ratio;
            double fat = nutrient.getFats() * ratio;

            tvPreview.setText(String.format(Locale.getDefault(),
                    "%.1f kcal | P: %.1fg | C: %.1fg | F: %.1fg",
                    cals, pro, carb, fat));
        } catch (NumberFormatException e) {
            tvPreview.setText("Invalid quantity");
        }
    }

    private void addAppUsageView(ScreenTimeDao.AppUsageSummary app) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_dashboard_app_usage, topAppsContainer, false);
        TextView tvAppName = view.findViewById(R.id.tvAppName);
        TextView tvAppUsage = view.findViewById(R.id.tvAppUsage);
        ImageView ivAppIcon = view.findViewById(R.id.ivAppIcon);

        PackageManager pm = getPackageManager();
        String name = app.packageName;
        try {
            ApplicationInfo ai = pm.getApplicationInfo(app.packageName, 0);
            name = pm.getApplicationLabel(ai).toString();
            ivAppIcon.setImageDrawable(ai.loadIcon(pm));
        } catch (PackageManager.NameNotFoundException e) {
            ivAppIcon.setImageResource(R.drawable.ic_placeholder);
        }

        tvAppName.setText(name);
        tvAppUsage.setText(DateUtils.convertTimestampIntoHoursAndMinutes(app.totalDuration));
        topAppsContainer.addView(view);
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
