package com.example.pp.features.analytics;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.features.analytics.adapter.CategoryExpenseAdapter;
import com.example.pp.features.analytics.chart.TrendMarkerView;
import com.example.pp.features.analytics.model.AnalyticsSummary;
import com.example.pp.features.analytics.model.CategoryExpense;
import com.example.pp.features.analytics.model.DailyExpense;
import com.example.pp.features.analytics.model.PeriodType;
import com.example.pp.features.settings.MoreActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AnalyticsActivity extends AppCompatActivity {

    private AnalyticsViewModel viewModel;
    private CategoryExpenseAdapter categoryAdapter;

    private TextView tvPeriodType, tvPeriodLabel, tvTotalExpenses, tvTransactionCount, tvAvgPerDay, tvHighestAmount, tvHighestCategoryName;
    private PieChart donutChart;
    private LineChart sparklineChart, trendChart;
    private RecyclerView rvCategories;
    private View periodDropdown, btnCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        viewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(AnalyticsViewModel.class);

        initViews();
        setupCharts();
        setupRecyclerView();
        setupControls();
        setupBottomNav();
        observeViewModel();

        viewModel.refreshAnalytics();
    }

    private void initViews() {
        tvPeriodType = findViewById(R.id.tvPeriodType);
        tvPeriodLabel = findViewById(R.id.tvPeriodLabel);
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses);
        tvTransactionCount = findViewById(R.id.tvTransactionCount);
        tvAvgPerDay = findViewById(R.id.tvAvgPerDay);
        tvHighestAmount = findViewById(R.id.tvHighestAmount);
        tvHighestCategoryName = findViewById(R.id.tvHighestCategoryName);
        donutChart = findViewById(R.id.donutChart);
        sparklineChart = findViewById(R.id.sparklineChart);
        trendChart = findViewById(R.id.trendChart);
        rvCategories = findViewById(R.id.rvCategories);
        periodDropdown = findViewById(R.id.periodDropdown);
        btnCalendar = findViewById(R.id.btnCalendar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupCharts() {
        // Donut chart
        donutChart.setUsePercentValues(true);
        donutChart.getDescription().setEnabled(false);
        donutChart.setDrawHoleEnabled(true);
        donutChart.setHoleColor(Color.TRANSPARENT);
        donutChart.setHoleRadius(58f);
        donutChart.setTransparentCircleRadius(61f);
        donutChart.setDrawCenterText(true);
        donutChart.setCenterTextColor(Color.WHITE);
        donutChart.setCenterTextSize(12f);
        donutChart.getLegend().setEnabled(false);
        donutChart.setDrawEntryLabels(true);
        donutChart.setEntryLabelColor(Color.WHITE);
        donutChart.setEntryLabelTextSize(10f);
        donutChart.setExtraOffsets(24f, 8f, 24f, 8f);
        donutChart.setTouchEnabled(false);

        // Sparkline (overview mini line)
        sparklineChart.getDescription().setEnabled(false);
        sparklineChart.setTouchEnabled(false);
        sparklineChart.setDrawGridBackground(false);
        sparklineChart.getAxisLeft().setEnabled(false);
        sparklineChart.getAxisRight().setEnabled(false);
        sparklineChart.getXAxis().setEnabled(false);
        sparklineChart.getLegend().setEnabled(false);

        // Trend chart
        trendChart.getDescription().setEnabled(false);
        trendChart.setTouchEnabled(true);
        trendChart.setDrawGridBackground(false);
        trendChart.getLegend().setEnabled(false);
        trendChart.getAxisRight().setEnabled(false);
        XAxis xAxis = trendChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.parseColor("#7B7B8C"));
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(6, true);
        YAxis yAxis = trendChart.getAxisLeft();
        yAxis.setTextColor(Color.parseColor("#7B7B8C"));
        yAxis.setGridColor(Color.parseColor("#2B2B3A"));
        yAxis.setDrawGridLines(true);
        yAxis.setAxisMinimum(0f);

        TrendMarkerView marker = new TrendMarkerView(this);
        marker.setChartView(trendChart);
        trendChart.setMarker(marker);
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryExpenseAdapter(R.layout.item_analytics_category_grid);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float dpWidth = dm.widthPixels / dm.density;
        int spanCount = Math.max(2, (int) (dpWidth / 140));
        rvCategories.setLayoutManager(new GridLayoutManager(this, spanCount));
        rvCategories.setAdapter(categoryAdapter);
        rvCategories.setNestedScrollingEnabled(false);
        categoryAdapter.setOnItemClickListener(item -> {
            viewModel.loadDrillDown(item.getCategoryId());
            showDrillDownBottomSheet();
        });
    }

    private void setupControls() {
        periodDropdown.setOnClickListener(v -> showPeriodTypePopup());
        btnCalendar.setOnClickListener(v -> showCalendarPicker());
    }

    private PopupWindow periodPopup;

    private void showPeriodTypePopup() {
        if (periodPopup != null && periodPopup.isShowing()) {
            periodPopup.dismiss();
            return;
        }
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_period_type, null);
        periodPopup = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        periodPopup.setElevation(8f);
        periodPopup.setBackgroundDrawable(null);
        periodPopup.showAsDropDown(periodDropdown, 0, 8);

        TextView tvMonth = popupView.findViewById(R.id.tvOptionMonth);
        TextView tvYear = popupView.findViewById(R.id.tvOptionYear);
        tvMonth.setOnClickListener(v -> {
            viewModel.setPeriodType(PeriodType.MONTH);
            periodPopup.dismiss();
        });
        tvYear.setOnClickListener(v -> {
            viewModel.setPeriodType(PeriodType.YEAR);
            periodPopup.dismiss();
        });
    }

    private void showCalendarPicker() {
        PeriodType type = viewModel.getPeriodType().getValue();
        if (type == PeriodType.MONTH) {
            showMonthYearPicker();
        } else {
            showYearPicker();
        }
    }

    private void showMonthYearPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.component_neon_dropdown, null);
        // We'll use a simple custom layout built in code since we don't have a dedicated month picker layout
        NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"});
        NumberPicker yearPicker = new NumberPicker(this);
        yearPicker.setMinValue(2020);
        yearPicker.setMaxValue(2030);

        Calendar cal = Calendar.getInstance();
        Integer currentYear = viewModel.getSelectedYear().getValue();
        Integer currentMonth = viewModel.getSelectedMonth().getValue();
        monthPicker.setValue(currentMonth != null ? currentMonth : cal.get(Calendar.MONTH));
        yearPicker.setValue(currentYear != null ? currentYear : cal.get(Calendar.YEAR));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(32, 32, 32, 32);
        layout.addView(monthPicker, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        layout.addView(yearPicker, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        builder.setView(layout);
        builder.setTitle("Select Month & Year");
        builder.setPositiveButton("OK", (dialog, which) -> {
            viewModel.setSelectedMonth(yearPicker.getValue(), monthPicker.getValue());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showYearPicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        NumberPicker yearPicker = new NumberPicker(this);
        yearPicker.setMinValue(2020);
        yearPicker.setMaxValue(2030);
        Integer currentYear = viewModel.getSelectedYear().getValue();
        yearPicker.setValue(currentYear != null ? currentYear : Calendar.getInstance().get(Calendar.YEAR));

        builder.setView(yearPicker);
        builder.setTitle("Select Year");
        builder.setPositiveButton("OK", (dialog, which) -> {
            viewModel.setSelectedYear(yearPicker.getValue());
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void setupBottomNav() {
        View navDashboard = findViewById(R.id.navDashboard);
        View navHistory = findViewById(R.id.navHistory);
        View navMore = findViewById(R.id.navMore);
        View btnCenterFab = findViewById(R.id.btnCenterFab);

        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.example.pp.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            });
        }
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                Toast.makeText(this, "History coming soon", Toast.LENGTH_SHORT).show();
            });
        }
        if (navMore != null) {
            navMore.setOnClickListener(v -> startActivity(new Intent(this, MoreActivity.class)));
        }
        if (btnCenterFab != null) {
            btnCenterFab.setOnClickListener(v -> startActivity(new Intent(this, com.example.pp.features.tracker.edit.EditTrackerActivity.class)));
        }
    }

    private void observeViewModel() {
        viewModel.getPeriodType().observe(this, type -> {
            tvPeriodType.setText(type == PeriodType.MONTH ? "Month" : "Year");
        });

        viewModel.getPeriodLabel().observe(this, label -> {
            tvPeriodLabel.setText(label);
        });

        viewModel.getAnalyticsSummary().observe(this, this::bindSummary);
        viewModel.getCategoryExpenses().observe(this, this::bindCategoryExpenses);
        viewModel.getDailyTrend().observe(this, this::bindTrend);
        viewModel.getDrillDownExpenses().observe(this, this::bindDrillDown);
    }

    private BottomSheetDialog drillDownDialog;
    private CategoryExpenseAdapter drillDownAdapter;

    private void showDrillDownBottomSheet() {
        if (drillDownDialog != null && drillDownDialog.isShowing()) return;
        drillDownDialog = new BottomSheetDialog(this, R.style.RoundedBottomSheetDialog);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_drill_down, null);
        TextView tvTitle = sheet.findViewById(R.id.tvDrillDownTitle);
        TextView tvSubtitle = sheet.findViewById(R.id.tvDrillDownSubtitle);
        RecyclerView rvDrillDown = sheet.findViewById(R.id.rvDrillDown);

        viewModel.getDrillDownTitle().observe(this, title -> {
            if (tvTitle != null) tvTitle.setText(title + " Breakdown");
            if (tvSubtitle != null) tvSubtitle.setText("Expenses by subcategory in " + viewModel.getPeriodLabel().getValue());
        });

        drillDownAdapter = new CategoryExpenseAdapter(R.layout.item_analytics_category_grid);
        rvDrillDown.setLayoutManager(new LinearLayoutManager(this));
        rvDrillDown.setAdapter(drillDownAdapter);

        drillDownDialog.setContentView(sheet);
        drillDownDialog.show();
    }

    private void bindDrillDown(List<CategoryExpense> expenses) {
        if (drillDownAdapter != null) {
            drillDownAdapter.submitList(expenses != null ? expenses : new ArrayList<>());
        }
    }

    private void bindSummary(AnalyticsSummary summary) {
        if (summary == null) return;
        tvTotalExpenses.setText(String.format(Locale.getDefault(), "\u20B9 %,.0f", summary.getTotalExpenses()));
        tvTransactionCount.setText(String.valueOf(summary.getTransactionCount()));
        tvAvgPerDay.setText(String.format(Locale.getDefault(), "\u20B9 %,.0f", summary.getAvgPerDay()));
        tvHighestAmount.setText(String.format(Locale.getDefault(), "\u20B9 %,.0f", summary.getHighestCategoryAmount()));
        tvHighestCategoryName.setText(summary.getHighestCategoryName());
        try {
            tvHighestCategoryName.setTextColor(Color.parseColor(summary.getHighestCategoryColour()));
        } catch (Exception e) {
            tvHighestCategoryName.setTextColor(Color.parseColor("#38BDF8"));
        }
    }

    private void bindCategoryExpenses(List<CategoryExpense> expenses) {
        if (expenses == null) expenses = new ArrayList<>();
        categoryAdapter.submitList(expenses);

        // Update donut chart
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        double total = 0;
        for (CategoryExpense ce : expenses) total += ce.getAmount();

        for (CategoryExpense ce : expenses) {
            float val = (float) ce.getAmount();
            // Category name is passed as the label
            entries.add(new PieEntry(val, ce.getName()));
            try {
                colors.add(Color.parseColor(ce.getColour()));
            } catch (Exception e) {
                colors.add(ColorTemplate.MATERIAL_COLORS[entries.size() % ColorTemplate.MATERIAL_COLORS.length]);
            }
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(7f);
        
        // Data labels outside the slices
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        
        // Improve spacing for data labels
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.5f);
        dataSet.setValueLineColor(Color.parseColor("#7B7B8C"));
        dataSet.setValueLineWidth(1f);
        dataSet.setValueLineVariableLength(true);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                // Return empty to only show the Category Name (Entry Label)
                return "";
            }
        });
        
        donutChart.setData(data);
        donutChart.setUsePercentValues(false);
        donutChart.setDrawEntryLabels(true);
        donutChart.setEntryLabelColor(Color.WHITE);
        donutChart.setEntryLabelTextSize(11f);
        
        // Center text
        donutChart.setCenterText(String.format(Locale.getDefault(), "\u20B9 %,.0f\nTotal\n%s", total, viewModel.getPeriodLabel().getValue() != null ? viewModel.getPeriodLabel().getValue() : ""));
        donutChart.invalidate();

        // Update sparkline
        bindSparkline(expenses);
    }

    private void bindSparkline(List<CategoryExpense> expenses) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < expenses.size(); i++) {
            entries.add(new Entry(i, (float) expenses.get(i).getAmount()));
        }
        LineDataSet set = new LineDataSet(entries, "");
        set.setColor(Color.parseColor("#A855F7"));
        set.setLineWidth(1.5f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        sparklineChart.setData(new LineData(set));
        sparklineChart.invalidate();
    }

    private void bindTrend(List<DailyExpense> trend) {
        if (trend == null || trend.isEmpty()) {
            trendChart.clear();
            return;
        }
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Long> timestamps = new ArrayList<>();
        PeriodType type = viewModel.getPeriodType().getValue();
        SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());

        int prevMonth = -1;
        for (int i = 0; i < trend.size(); i++) {
            DailyExpense de = trend.get(i);
            entries.add(new Entry(i, (float) de.getAmount()));
            timestamps.add(de.getDateTimestamp());

            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(de.getDateTimestamp());

            if (type == PeriodType.YEAR) {
                int month = cal.get(Calendar.MONTH);
                if (i == 0 || month != prevMonth) {
                    labels.add(monthFormat.format(cal.getTime()));
                    prevMonth = month;
                } else {
                    labels.add("");
                }
            } else {
                // Month mode: show every 5th day roughly
                if (i % Math.max(1, trend.size() / 6) == 0) {
                    labels.add(dayFormat.format(cal.getTime()));
                } else {
                    labels.add("");
                }
            }
        }

        LineDataSet set = new LineDataSet(entries, "");
        set.setColor(Color.parseColor("#A855F7"));
        set.setLineWidth(2f);
        set.setDrawCircles(true);
        set.setCircleColor(Color.parseColor("#EC4899"));
        set.setCircleRadius(3f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setFillColor(Color.parseColor("#A855F733"));
        set.setDrawFilled(true);
        set.setHighlightLineWidth(1.5f);
        set.setHighLightColor(Color.parseColor("#FACC15"));

        trendChart.setData(new LineData(set));
        trendChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        TrendMarkerView marker = (TrendMarkerView) trendChart.getMarker();
        if (marker != null) {
            marker.setDateTimestamps(timestamps);
            marker.setDatePattern(type == PeriodType.YEAR ? "d MMM" : "d MMM");
        }
        trendChart.invalidate();
    }
}

