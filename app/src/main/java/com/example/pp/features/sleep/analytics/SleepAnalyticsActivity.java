package com.example.pp.features.sleep.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.core.enums.PeriodType;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.repository.SleepRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SleepAnalyticsActivity extends AppCompatActivity {

    private SleepRepository repository;
    private BarChart sleepChart;
    private TextView tvChartLabel;
    private PeriodType currentPeriod = PeriodType.WEEK;
    private LiveData<List<SleepEntity>> currentData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_analytics);

        repository = ((PPApplication) getApplication()).getSleepRepository();
        initViews();
        setupChart();
        updateRange(PeriodType.WEEK);
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        sleepChart = findViewById(R.id.sleepChart);
        tvChartLabel = findViewById(R.id.tvChartLabel);
        MaterialButtonToggleGroup togglePeriod = findViewById(R.id.togglePeriod);

        togglePeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnWeek) updateRange(PeriodType.WEEK);
                else if (checkedId == R.id.btnMonth) updateRange(PeriodType.MONTH);
            }
        });
    }

    private void updateRange(PeriodType period) {
        currentPeriod = period;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        long end = System.currentTimeMillis() + 86400000L;
        long start;

        if (period == PeriodType.WEEK) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            start = cal.getTimeInMillis();
            tvChartLabel.setText("Weekly Sleep Trend");
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            start = cal.getTimeInMillis();
            tvChartLabel.setText("Monthly Sleep Trend");
        }

        if (currentData != null) currentData.removeObservers(this);
        currentData = repository.getSleepHistory(start, end);
        currentData.observe(this, this::updateChart);
    }

    private void setupChart() {
        sleepChart.getDescription().setEnabled(false);
        sleepChart.getLegend().setEnabled(false);
        sleepChart.setDrawGridBackground(false);

        XAxis xAxis = sleepChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGranularity(1f);

        YAxis leftAxis = sleepChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(12f); // Max 12 hours for scale
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "h";
            }
        });

        sleepChart.getAxisRight().setEnabled(false);
    }

    private void updateChart(List<SleepEntity> data) {
        if (data == null) return;

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        if (currentPeriod == PeriodType.WEEK) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            for (int i = 0; i < 7; i++) {
                long dayStart = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_YEAR, 1);
                long dayEnd = cal.getTimeInMillis();
                
                float totalHours = 0;
                for (SleepEntity s : data) {
                    if (s.startTime >= dayStart && s.startTime < dayEnd && s.endTime != 0) {
                        totalHours += (s.endTime - s.startTime) / (1000f * 60 * 60);
                    }
                }
                entries.add(new BarEntry(i, totalHours));
                labels.add(new SimpleDateFormat("EEE", Locale.getDefault()).format(dayStart));
            }
        } else {
            int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            for (int i = 0; i < maxDays; i++) {
                long dayStart = cal.getTimeInMillis();
                cal.add(Calendar.DAY_OF_YEAR, 1);
                long dayEnd = cal.getTimeInMillis();

                float totalHours = 0;
                for (SleepEntity s : data) {
                    if (s.startTime >= dayStart && s.startTime < dayEnd && s.endTime != 0) {
                        totalHours += (s.endTime - s.startTime) / (1000f * 60 * 60);
                    }
                }
                entries.add(new BarEntry(i, totalHours));
                labels.add(String.valueOf(i + 1));
            }
        }

        BarDataSet set = new BarDataSet(entries, "Sleep Hours");
        set.setColor(ContextCompat.getColor(this, R.color.accent_purple));
        set.setDrawValues(false);

        BarData barData = new BarData(set);
        sleepChart.setData(barData);
        sleepChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
        sleepChart.invalidate();
    }
}
