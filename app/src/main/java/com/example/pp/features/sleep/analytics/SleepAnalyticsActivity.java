package com.example.pp.features.sleep.analytics;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.pp.R;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.PeriodType;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.features.analytics.chart.TimeMarkerView;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SleepAnalyticsActivity extends AppCompatActivity {

    private SleepAnalyticsViewModel viewModel;
    private BarChart sleepChart;
    private TextView tvPeriodLabel, tvChartLabel, tvPeriodType;
    private TextView tvAvgValue, tvPercentChange;
    private View periodDropdown, btnCalendar;

    private View selectedSleepDetails, reasonContainer, editReasonContainer;
    private TextView tvSelectedDate, tvSelectedRange, tvSelectedDuration, tvReason;
    private EditText etReason;
    private ImageView btnEditReason;
    private TextView btnSaveReason;
    private long lastSelectedDate = 0;

    private static final long TARGET_8H_MS = 8 * 3600 * 1000L;
    private static final long DANGER_6H_MS = 6 * 3600 * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_analytics);

        viewModel = new ViewModelProvider(this).get(SleepAnalyticsViewModel.class);

        initViews();
        setupChart();
        observeViewModel();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        sleepChart = findViewById(R.id.sleepChart);
        tvPeriodLabel = findViewById(R.id.tvPeriodLabel);
        tvChartLabel = findViewById(R.id.tvChartLabel);
        tvPeriodType = findViewById(R.id.tvPeriodType);
        tvAvgValue = findViewById(R.id.tvAvgValue);
        tvPercentChange = findViewById(R.id.tvPercentChange);
        periodDropdown = findViewById(R.id.periodDropdown);
        btnCalendar = findViewById(R.id.btnCalendar);

        findViewById(R.id.btnPrev).setOnClickListener(v -> viewModel.navigatePeriod(-1));
        findViewById(R.id.btnNext).setOnClickListener(v -> viewModel.navigatePeriod(1));

        periodDropdown.setOnClickListener(v -> showPeriodPopup());
        btnCalendar.setOnClickListener(v -> showDatePicker());

        // Initialize Selection Details Views
        selectedSleepDetails = findViewById(R.id.selectedSleepDetails);
        reasonContainer = findViewById(R.id.reasonContainer);
        editReasonContainer = findViewById(R.id.editReasonContainer);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvSelectedRange = findViewById(R.id.tvSelectedRange);
        tvSelectedDuration = findViewById(R.id.tvSelectedDuration);
        tvReason = findViewById(R.id.tvReason);
        etReason = findViewById(R.id.etReason);
        btnEditReason = findViewById(R.id.btnEditReason);
        btnSaveReason = findViewById(R.id.btnSaveReason);

        btnEditReason.setOnClickListener(v -> {
            reasonContainer.setVisibility(View.GONE);
            editReasonContainer.setVisibility(View.VISIBLE);
            etReason.requestFocus();
        });

        btnSaveReason.setOnClickListener(v -> {
            String newReason = etReason.getText().toString().trim();
            if (lastSelectedDate != 0) {
                viewModel.updateDelayReason(lastSelectedDate, newReason);
                tvReason.setText(newReason.isEmpty() ? "No reason added" : newReason);
                reasonContainer.setVisibility(View.VISIBLE);
                editReasonContainer.setVisibility(View.GONE);
                Toast.makeText(this, "Reason updated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupChart() {
        sleepChart.getDescription().setEnabled(false);
        sleepChart.setDrawGridBackground(false);
        sleepChart.setDrawBarShadow(false);
        sleepChart.getLegend().setEnabled(false);

        XAxis xAxis = sleepChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGranularity(1f);

        YAxis leftAxis = sleepChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.stroke_color));
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(12 * 3600 * 1000f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) (value / (3600 * 1000)) + "h";
            }
        });

        sleepChart.getAxisRight().setEnabled(false);

        // Add Target Line
        leftAxis.removeAllLimitLines();
        LimitLine ll = new LimitLine(TARGET_8H_MS, "Target 8h");
        ll.setLineColor(Color.WHITE);
        ll.setLineWidth(1f);
        ll.enableDashedLine(10f, 10f, 0f);
        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll.setTextColor(Color.WHITE);
        leftAxis.addLimitLine(ll);

        sleepChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int) e.getX();
                long start = viewModel.getRangeStart();
                long selectedDate = start + index * 24 * 3600 * 1000L;
                lastSelectedDate = selectedDate;
                
                showSleepDetails(selectedDate, (long) e.getY());
            }

            @Override
            public void onNothingSelected() {
                selectedSleepDetails.setVisibility(View.GONE);
                lastSelectedDate = 0;
            }
        });
    }

    private void showSleepDetails(long date, long duration) {
        List<SleepEntity> data = viewModel.getSleepData().getValue();
        SleepEntity selectedEntity = null;
        if (data != null) {
            for (SleepEntity s : data) {
                if (s.date == date) {
                    selectedEntity = s;
                    break;
                }
            }
        }

        selectedSleepDetails.setVisibility(View.VISIBLE);
        reasonContainer.setVisibility(View.VISIBLE);
        editReasonContainer.setVisibility(View.GONE);

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvSelectedDate.setText(sdfDate.format(new java.util.Date(date)));
        tvSelectedDuration.setText(formatMillis(duration));

        if (selectedEntity != null && selectedEntity.startTime != 0 && selectedEntity.endTime != 0) {
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String range = sdfTime.format(new java.util.Date(selectedEntity.startTime)) + " - " + 
                          sdfTime.format(new java.util.Date(selectedEntity.endTime));
            tvSelectedRange.setText(range);
        } else {
            tvSelectedRange.setText("No sleep data logged");
        }

        String reason = (selectedEntity != null) ? selectedEntity.getDelayReason() : "";
        tvReason.setText(reason == null || reason.isEmpty() ? "No reason added" : reason);
        etReason.setText(reason);
    }

    private void observeViewModel() {
        viewModel.getPeriodLabel().observe(this, tvPeriodLabel::setText);
        viewModel.getPeriodType().observe(this, type -> {
            String typeStr = type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase();
            tvPeriodType.setText(typeStr);
            tvChartLabel.setText(typeStr + "ly Sleep Trend");
        });

        viewModel.getStats().observe(this, s -> {
            tvAvgValue.setText(formatMillis((long) s.avgValue));
            String change = String.format(Locale.getDefault(), "%.1f%%", Math.abs(s.percentChange));
            tvPercentChange.setText((s.percentChange >= 0 ? "+" : "-") + change);
            tvPercentChange.setTextColor(ContextCompat.getColor(this, s.percentChange >= 0 ? R.color.emerald_green : R.color.error_red));
        });

        viewModel.getSleepData().observe(this, this::renderChart);
    }

    private void renderChart(List<SleepEntity> data) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        PeriodType type = viewModel.getPeriodType().getValue();
        long start = viewModel.getRangeStart();
        long end = viewModel.getRangeEnd();

        Map<Long, Long> dailySleep = new HashMap<>();
        if (data != null) {
            for (SleepEntity s : data) {
                long day = s.date;
                long duration = (s.endTime > s.startTime) ? (s.endTime - s.startTime) : 0;
                dailySleep.put(day, dailySleep.getOrDefault(day, 0L) + duration);
            }
        }

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(start);
        int i = 0;
        SimpleDateFormat sdf = new SimpleDateFormat(type == PeriodType.WEEK ? "dd MMM" : "dd", Locale.getDefault());
        while (c.getTimeInMillis() < end) {
            long dStart = c.getTimeInMillis();
            long duration = dailySleep.getOrDefault(dStart, 0L);
            
            entries.add(new BarEntry(i, duration));
            labels.add(sdf.format(dStart));
            
            if (duration == 0) {
                colors.add(Color.TRANSPARENT);
            } else if (duration < DANGER_6H_MS) {
                // Poor sleep
                colors.add(ContextCompat.getColor(this, R.color.nutrition_dark_purple));
            } else if (duration < TARGET_8H_MS) {
                // Fair sleep
                colors.add(ContextCompat.getColor(this, R.color.accent_purple));
            } else {
                // Excellent sleep
                colors.add(ContextCompat.getColor(this, R.color.glow_border_purple));
            }
            
            c.add(Calendar.DAY_OF_YEAR, 1);
            i++;
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(colors);
        // Show labels only for Weekly and only if below 8h
        set.setDrawValues(type == PeriodType.WEEK);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);
        set.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (type == PeriodType.WEEK && value > 0 && value < TARGET_8H_MS) {
                    return String.format(Locale.getDefault(), "%.1fh", value / (3600 * 1000f));
                }
                return "";
            }
        });

        sleepChart.setData(new BarData(set));
        sleepChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        sleepChart.animateY(1000);
        
        TimeMarkerView marker = new TimeMarkerView(this);
        marker.setLabels(labels.toArray(new String[0]));
        marker.setDisplayMode(TimeMarkerView.DisplayMode.TIME);
        sleepChart.setMarker(marker);
        
        sleepChart.invalidate();
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

    private String formatMillis(long millis) {
        long hours = millis / (3600 * 1000);
        long mins = (millis % (3600 * 1000)) / (60 * 1000);
        return hours + "h " + mins + "m";
    }
}
