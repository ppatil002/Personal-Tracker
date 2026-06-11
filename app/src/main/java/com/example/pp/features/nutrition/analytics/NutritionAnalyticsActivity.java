package com.example.pp.features.nutrition.analytics;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.core.enums.PeriodType;
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

    private static final float TARGET_CALORIES = 2000f;

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
        LimitLine ll = new LimitLine(TARGET_CALORIES, "Target 2k");
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

    private void renderChart(List<NutritionIntakeWithFood> data) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        PeriodType type = viewModel.getPeriodType().getValue();
        long start = viewModel.getRangeStart();
        long end = viewModel.getRangeEnd();

        Map<Long, Double> dailyTotals = new HashMap<>();
        if (data != null) {
            for (NutritionIntakeWithFood item : data) {
                if (item.food == null) continue;
                long day = DateUtils.getStartOfDay(item.intake.intakeTimestamp);
                double calories = (item.intake.quantity / item.food.getPerUnit()) * item.food.getCalories();
                dailyTotals.put(day, dailyTotals.getOrDefault(day, 0.0) + calories);
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
                colors.add(Color.parseColor("#4C1D95")); // Low
            } else if (total < TARGET_CALORIES) {
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
        List<NutritionIntakeWithFood> data = viewModel.getNutritionData().getValue();
        List<NutritionIntakeWithFood> filtered = new ArrayList<>();
        if (data != null) {
            for (NutritionIntakeWithFood item : data) {
                if (DateUtils.getStartOfDay(item.intake.intakeTimestamp) == date) {
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

        for (int i = 0; i < filtered.size(); i++) {
            NutritionIntakeWithFood current = filtered.get(i);
            
            if (i > 0) {
                NutritionIntakeWithFood prev = filtered.get(i - 1);
                long gapMs = current.intake.intakeTimestamp - prev.intake.intakeTimestamp;
                String gapLabel = formatGap(gapMs);
                boolean isWarning = gapMs > TimeUnit.HOURS.toMillis(5);
                timelineItems.add(new TimelineAdapter.GapItem(gapLabel + (isWarning ? " gap detected" : " elapsed"), isWarning));
            }

            double calories = 0;
            String foodName = "Unknown";
            if (current.food != null) {
                calories = (current.intake.quantity / current.food.getPerUnit()) * current.food.getCalories();
                foodName = current.food.getFoodItem();
            }

            boolean isRecent = (System.currentTimeMillis() - current.intake.intakeTimestamp) < TimeUnit.MINUTES.toMillis(30);

            timelineItems.add(new TimelineAdapter.IntakeItem(
                    sdfTime.format(new Date(current.intake.intakeTimestamp)),
                    String.format(Locale.getDefault(), "%.0f kcal", calories),
                    foodName + " (" + (int)current.intake.quantity + (current.food != null ? current.food.getUnit() : "") + ")",
                    R.drawable.ic_restaurant,
                    R.color.nutrition_purple,
                    isRecent
            ));
        }

        timelineAdapter.setItems(timelineItems);
    }

    private String formatGap(long ms) {
        long mins = TimeUnit.MILLISECONDS.toMinutes(ms);
        if (mins < 60) return mins + "m";
        long hrs = mins / 60;
        mins %= 60;
        if (mins == 0) return hrs + "h";
        return hrs + "h " + mins + "m";
    }

    private void showPeriodPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_period_type_sleep, null);
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
        monthPicker.setDisplayedValues(new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"});
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
