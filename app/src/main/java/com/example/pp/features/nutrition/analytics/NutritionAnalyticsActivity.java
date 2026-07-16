package com.example.pp.features.nutrition.analytics;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.PeriodType;
import com.example.pp.data.local.model.GroupedNutritionIntake;
import com.example.pp.data.local.model.NutritionIntakeWithFood;
import com.example.pp.ui.components.timeline.TimelineAdapter;
import com.example.pp.utils.DateUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NutritionAnalyticsActivity extends AppCompatActivity {

    private NutritionAnalyticsViewModel viewModel;
    private BarChart nutritionChart;
    private TextView tvPeriodLabel, tvChartLabel, tvPeriodType;
    private View periodDropdown, btnCalendar;
    
    private View timelineCard;
    private RecyclerView rvTimeline;
    private TimelineAdapter timelineAdapter;
    private TextView tvTimelineTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nutrition_analytics);

        viewModel = new ViewModelProvider(this).get(NutritionAnalyticsViewModel.class);

        initViews();
        setupChart();
        observeViewModel();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        nutritionChart = findViewById(R.id.nutritionChart);
        tvPeriodLabel = findViewById(R.id.tvPeriodLabel);
        tvChartLabel = findViewById(R.id.tvChartLabel);
        tvPeriodType = findViewById(R.id.tvPeriodType);
        periodDropdown = findViewById(R.id.periodDropdown);
        btnCalendar = findViewById(R.id.btnCalendar);

        findViewById(R.id.btnPrev).setOnClickListener(v -> viewModel.navigatePeriod(-1));
        findViewById(R.id.btnNext).setOnClickListener(v -> viewModel.navigatePeriod(1));

        periodDropdown.setOnClickListener(v -> showPeriodPopup());
        btnCalendar.setOnClickListener(v -> showDatePicker());

        timelineCard = findViewById(R.id.timelineCard);
        rvTimeline = findViewById(R.id.rvTimeline);
        tvTimelineTitle = findViewById(R.id.tvTimelineTitle);
        
        timelineAdapter = new TimelineAdapter();
        rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        rvTimeline.setAdapter(timelineAdapter);

        timelineAdapter.setOnItemClickListener(this::showEditDialogue);
    }

    private void setupChart() {
        nutritionChart.getDescription().setEnabled(false);
        nutritionChart.setDrawGridBackground(false);
        nutritionChart.setDrawBarShadow(false);
        nutritionChart.getLegend().setEnabled(false);

        XAxis xAxis = nutritionChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGranularity(1f);

        YAxis leftAxis = nutritionChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.stroke_color));
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + " kcal";
            }
        });

        nutritionChart.getAxisRight().setEnabled(false);

        // Add Target Line
        LimitLine ll = new LimitLine((float) AppConstants.TARGET_CALORIES, "Target 2k");
        ll.setLineColor(Color.WHITE);
        ll.setLineWidth(1f);
        ll.enableDashedLine(10f, 10f, 0f);
        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll.setTextColor(Color.WHITE);
        leftAxis.addLimitLine(ll);

        nutritionChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                long start = viewModel.getRangeStart();
                long selectedDate = start + index * 24 * 3600 * 1000L;
                updateTimeline(selectedDate);
            }

            @Override
            public void onNothingSelected() {
                timelineCard.setVisibility(View.GONE);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getPeriodLabel().observe(this, tvPeriodLabel::setText);
        viewModel.getPeriodType().observe(this, type -> {
            String typeStr = type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase();
            tvPeriodType.setText(typeStr);
            tvChartLabel.setText(typeStr + "ly Calorie Trend");
        });

        viewModel.getNutritionData().observe(this, data -> {
            renderChart(data);
            
            // Default selection logic: 
            // If Today is in range, select Today. 
            // Otherwise, select the last day of the range.
            long now = DateUtils.getStartOfDay(System.currentTimeMillis());
            long start = viewModel.getRangeStart();
            long end = viewModel.getRangeEnd();
            
            if (now >= start && now < end) {
                updateTimeline(now);
            } else {
                // Select the last day of the period (end - 1 day)
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(end);
                cal.add(Calendar.DAY_OF_YEAR, -1);
                updateTimeline(cal.getTimeInMillis());
            }
        });
    }

    private void renderChart(List<GroupedNutritionIntake> data) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        PeriodType type = viewModel.getPeriodType().getValue();
        long start = viewModel.getRangeStart();
        long end = viewModel.getRangeEnd();

        Map<Long, Double> dailyTotals = new HashMap<>();
        if (data != null) {
            for (GroupedNutritionIntake item : data) {
                long day = DateUtils.getStartOfDay(item.intake_timestamp);
                dailyTotals.put(day, dailyTotals.getOrDefault(day, 0.0) + item.totalCalories);
            }
        }

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start);
        int i = 0;
        SimpleDateFormat sdf = new SimpleDateFormat(type == PeriodType.WEEK ? "dd MMM" : "dd", Locale.getDefault());

        while (c.getTimeInMillis() < end) {
            long dStart = c.getTimeInMillis();
            double total = dailyTotals.getOrDefault(dStart, 0.0);
            
            entries.add(new BarEntry(i, (float) total));
            labels.add(sdf.format(dStart));
            
            if (total == 0) {
                colors.add(Color.TRANSPARENT);
            } else if (total < 1200) {
                colors.add(ContextCompat.getColor(this, R.color.nutrition_dark_purple));
            } else if (total < AppConstants.TARGET_CALORIES) {
                colors.add(ContextCompat.getColor(this, R.color.nutrition_purple));
            } else {
                colors.add(ContextCompat.getColor(this, R.color.emerald_green));
            }
            
            c.add(Calendar.DAY_OF_YEAR, 1);
            i++;
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(colors);
        set.setDrawValues(false);

        nutritionChart.setData(new BarData(set));
        nutritionChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        nutritionChart.animateY(1000);
        nutritionChart.invalidate();
    }

    private void updateTimeline(long date) {
        List<GroupedNutritionIntake> data = viewModel.getNutritionData().getValue();
        List<GroupedNutritionIntake> filtered = new ArrayList<>();
        if (data != null) {
            for (GroupedNutritionIntake item : data) {
                if (DateUtils.getStartOfDay(item.intake_timestamp) == date) {
                    filtered.add(item);
                }
            }
        }

        if (filtered.isEmpty()) {
            timelineCard.setVisibility(View.GONE);
            return;
        }

        timelineCard.setVisibility(View.VISIBLE);
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM", Locale.getDefault());
        tvTimelineTitle.setText("Meal History - " + sdfDate.format(new Date(date)));

        List<TimelineAdapter.TimelineItem> timelineItems = new ArrayList<>();
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        double runningCal = 0, runningP = 0, runningC = 0, runningF = 0;

        for (int i = 0; i < filtered.size(); i++) {
            GroupedNutritionIntake current = filtered.get(i);
            runningCal += current.totalCalories;
            runningP += current.totalProtein;
            runningC += current.totalCarbs;
            runningF += current.totalFats;
            
            if (i > 0) {
                GroupedNutritionIntake prev = filtered.get(i - 1);
                long gapMs = current.intake_timestamp - prev.intake_timestamp;
                String gapLabel = formatGap(gapMs);
                boolean isWarning = gapMs > TimeUnit.HOURS.toMillis(3);
                timelineItems.add(new TimelineAdapter.GapItem(gapLabel + (isWarning ? " gap detected" : " elapsed"), isWarning));
            }

            boolean isRecent = (System.currentTimeMillis() - current.intake_timestamp) < TimeUnit.MINUTES.toMillis(30);

            String title = getDisplayTag(current.tags);

            String stats = String.format(Locale.getDefault(),
                    "kCal: %.0f (%.0f%%), P: %.1f (%.0f%%)\nC: %.1f (%.0f%%), F: %.1f (%.0f%%)",
                    current.totalCalories, (runningCal / AppConstants.TARGET_CALORIES) * 100,
                    current.totalProtein, (runningP / AppConstants.TARGET_PROTEIN) * 100,
                    current.totalCarbs, (runningC / AppConstants.TARGET_CARBS) * 100,
                    current.totalFats, (runningF / AppConstants.TARGET_FATS) * 100);

            timelineItems.add(new TimelineAdapter.IntakeItem(
                    sdfTime.format(new Date(current.intake_timestamp)),
                    title,
                    current.foodItems,
                    stats,
                    isRecent,
                    current
            ));
        }

        timelineAdapter.setItems(timelineItems);
    }

    private String getDisplayTag(String tags) {
        if (tags == null || tags.trim().isEmpty()) return "Meal";
        String[] parts = tags.split(",\\s*");
        java.util.LinkedHashSet<String> uniqueTags = new java.util.LinkedHashSet<>();
        for (String p : parts) {
            String clean = p.trim();
            if (!clean.isEmpty()) uniqueTags.add(clean);
        }
        if (uniqueTags.isEmpty()) return "Meal";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : uniqueTags) {
            if (!first) sb.append(", ");
            sb.append(s);
            first = false;
        }
        return sb.toString();
    }

    private String formatGap(long ms) {
        long mins = TimeUnit.MILLISECONDS.toMinutes(ms);
        if (mins < 60) return mins + "m";
        long hrs = mins / 60;
        mins %= 60;
        if (mins == 0) return hrs + "h";
        return hrs + "h " + mins + "m";
    }

    private void showEditDialogue(TimelineAdapter.TimelineItem item) {
        if (!(item instanceof TimelineAdapter.IntakeItem)) return;
        TimelineAdapter.IntakeItem intakeItem = (TimelineAdapter.IntakeItem) item;
        if (!(intakeItem.originalData instanceof GroupedNutritionIntake)) return;
        
        GroupedNutritionIntake group = (GroupedNutritionIntake) intakeItem.originalData;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_nutrition_group, null);

        TextView tvTime = view.findViewById(R.id.tvEditTime);
        LinearLayout itemsContainer = view.findViewById(R.id.itemsContainer);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(group.intake_timestamp);
        SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvTime.setText("Time: " + sdfTime.format(cal.getTime()));

        tvTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                cal.set(Calendar.MINUTE, minute);
                tvTime.setText("Time: " + sdfTime.format(cal.getTime()));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
        });

        AlertDialog dialog = builder.setView(view).create();
        dialog.show();

        viewModel.getIntakesForGroup(group.intake_timestamp).observe(this, intakes -> {
            itemsContainer.removeAllViews();
            if (intakes == null || intakes.isEmpty()) {
                dialog.dismiss();
                return;
            }

            for (NutritionIntakeWithFood nutritionItem : intakes) {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_dialog_nutrition_edit, itemsContainer, false);
                TextView tvName = itemView.findViewById(R.id.tvFoodName);
                EditText etQty = itemView.findViewById(R.id.etQuantity);
                TextView tvUnit = itemView.findViewById(R.id.tvUnit);
                View btnDelete = itemView.findViewById(R.id.btnDelete);

                tvName.setText(nutritionItem.food.getFoodItem());
                etQty.setText(String.format(Locale.getDefault(), "%.1f", nutritionItem.intake.quantity));
                tvUnit.setText(nutritionItem.food.getUnit());

                etQty.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        etQty.post(() -> {
                            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                            if (imm != null) imm.showSoftInput(etQty, InputMethodManager.SHOW_IMPLICIT);
                        });
                    }
                });

                btnDelete.setOnClickListener(v -> {
                    viewModel.deleteIntake(nutritionItem.intake);
                });

                itemView.setTag(nutritionItem);
                itemsContainer.addView(itemView);
            }
        });

        view.findViewById(R.id.btnSave).setOnClickListener(v -> {
            // Update timestamp for all items in group
            viewModel.updateGroupTimestamp(group.intake_timestamp, cal.getTimeInMillis());

            // Update quantities
            for (int i = 0; i < itemsContainer.getChildCount(); i++) {
                View itemView = itemsContainer.getChildAt(i);
                NutritionIntakeWithFood nutritionItem = (NutritionIntakeWithFood) itemView.getTag();
                EditText etQty = itemView.findViewById(R.id.etQuantity);
                try {
                    double newQty = Double.parseDouble(etQty.getText().toString());
                    if (Math.abs(newQty - nutritionItem.intake.quantity) > 0.01) {
                        nutritionItem.intake.quantity = newQty;
                        viewModel.updateIntake(nutritionItem.intake);
                    }
                } catch (NumberFormatException ignored) {}
            }
            dialog.dismiss();
            Toast.makeText(this, "Intake updated", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private void showPeriodPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_week_month_period, null);
        PopupWindow popup = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setElevation(8f);
        popup.showAsDropDown(periodDropdown);

        popupView.findViewById(R.id.tvOptionWeek).setOnClickListener(v -> {
            viewModel.setPeriodType(PeriodType.WEEK);
            popup.dismiss();
        });
        popupView.findViewById(R.id.tvOptionMonth).setOnClickListener(v -> {
            viewModel.setPeriodType(PeriodType.MONTH);
            popup.dismiss();
        });
    }

    private void showDatePicker() {
        PeriodType type = viewModel.getPeriodType().getValue();
        if (type == PeriodType.MONTH) {
            showMonthYearPicker();
            return;
        }

        Long current = viewModel.getSelectedDate().getValue();
        if (current == null) current = System.currentTimeMillis();
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(current);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            viewModel.setSelectedDate(selected.getTimeInMillis());
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showMonthYearPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(AppConstants.MONTHS_OF_YEAR);
        NumberPicker yearPicker = new NumberPicker(this);
        yearPicker.setMinValue(2020);
        yearPicker.setMaxValue(2030);

        Calendar cal = Calendar.getInstance();
        Long current = viewModel.getSelectedDate().getValue();
        if (current != null) cal.setTimeInMillis(current);
        monthPicker.setValue(cal.get(Calendar.MONTH));
        yearPicker.setValue(cal.get(Calendar.YEAR));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(32, 32, 32, 32);
        layout.addView(monthPicker, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        layout.addView(yearPicker, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        builder.setView(layout);
        builder.setTitle("Select Month & Year");
        builder.setPositiveButton("OK", (dialog, which) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.YEAR, yearPicker.getValue());
            selected.set(Calendar.MONTH, monthPicker.getValue());
            selected.set(Calendar.DAY_OF_MONTH, 1);
            viewModel.setSelectedDate(selected.getTimeInMillis());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
