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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.features.analytics.adapter.CategoryExpenseAdapter;
import com.example.pp.features.analytics.adapter.TransactionAdapter;
import com.example.pp.features.analytics.chart.TrendMarkerView;
import com.example.pp.features.analytics.model.AnalyticsSummary;
import com.example.pp.features.analytics.model.CategoryExpense;
import com.example.pp.features.analytics.model.CategoryTrendData;
import com.example.pp.features.analytics.model.DailyExpense;
import com.example.pp.features.analytics.model.PeriodType;
import com.example.pp.ui.components.table.adapter.GenericTableAdapter;
import com.example.pp.ui.components.table.model.TableColumn;
import com.example.pp.ui.components.table.model.TableConfig;
import com.example.pp.ui.components.table.model.TableRow;
import com.example.pp.data.local.entity.CategoryEntity;
import com.example.pp.features.settings.MoreActivity;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
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
    private GenericTableAdapter tableAdapter, heatmapAdapter;
    private String currentRootCategoryId;

    private TextView tvPeriodType, tvPeriodLabel, tvTotalExpenses, tvTransactionCount, tvAvgPerDay, tvHighestAmount, tvHighestCategoryName;
    private TextView tvSelectedTableCategory, tvExpandTable;
    private View tableExpandableContainer, tableCategorySelector;
    private LinearLayout tableHeaderContainer, tableGroupHeaderContainer, heatmapHeaderContainer;
    private com.google.android.material.chip.ChipGroup tagChipGroup;
    private PieChart donutChart;
    private LineChart sparklineChart, trendChart;
    private RecyclerView rvCategories, rvDrillDownTable, rvHeatmap;
    private LinearLayout categoryTrendChartsContainer;
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
        rvDrillDownTable = findViewById(R.id.rvDrillDownTable);
        rvHeatmap = findViewById(R.id.rvHeatmap);
        
        tvSelectedTableCategory = findViewById(R.id.tvSelectedTableCategory);
        tvExpandTable = findViewById(R.id.tvExpandTable);
        tableExpandableContainer = findViewById(R.id.tableExpandableContainer);
        tableCategorySelector = findViewById(R.id.tableCategorySelector);
        tableHeaderContainer = findViewById(R.id.tableHeaderContainer);
        tableGroupHeaderContainer = findViewById(R.id.tableGroupHeaderContainer);
        heatmapHeaderContainer = findViewById(R.id.heatmapHeaderContainer);
        tagChipGroup = findViewById(R.id.tagChipGroup);
        categoryTrendChartsContainer = findViewById(R.id.categoryTrendChartsContainer);
        
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
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(6, true);
        YAxis yAxis = trendChart.getAxisLeft();
        yAxis.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        yAxis.setGridColor(ContextCompat.getColor(this, R.color.border_color));
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

        tableAdapter = new GenericTableAdapter(new TableConfig());
        tableAdapter.setFixedCellWidth((int) (80 * getResources().getDisplayMetrics().density));
        tableAdapter.setOnRowClickListener(row -> {
            if (row.getRowId().contains("footer")) return;
            Long timestamp = (Long) row.getOriginalEntity();
            if (timestamp != null && currentRootCategoryId != null) {
                viewModel.loadTransactionsForPeriod(timestamp, currentRootCategoryId);
                showTransactionListBottomSheet(timestamp);
            }
        });
        rvDrillDownTable.setLayoutManager(new LinearLayoutManager(this));
        rvDrillDownTable.setAdapter(tableAdapter);

        heatmapAdapter = new GenericTableAdapter(new TableConfig());
        heatmapAdapter.setFixedCellWidth((int) (80 * getResources().getDisplayMetrics().density));
        rvHeatmap.setLayoutManager(new LinearLayoutManager(this));
        rvHeatmap.setAdapter(heatmapAdapter);
    }

    private void setupControls() {
        periodDropdown.setOnClickListener(v -> showPeriodTypePopup());
        btnCalendar.setOnClickListener(v -> showCalendarPicker());
        
        findViewById(R.id.cardDrillDown).setOnClickListener(v -> {
            boolean isExpanded = tableExpandableContainer.getVisibility() == View.VISIBLE;
            viewModel.setTableExpanded(!isExpanded);
        });

        tableCategorySelector.setOnClickListener(v -> showTableCategoryPopup());
    }

    private void showTableCategoryPopup() {
        List<CategoryEntity> categories = viewModel.getRootCategories().getValue();
        if (categories == null || categories.isEmpty()) return;

        String[] names = new String[categories.size() + 1];
        names[0] = "All Category";
        for (int i = 0; i < categories.size(); i++) names[i + 1] = categories.get(i).getName();

        new AlertDialog.Builder(this)
                .setTitle("Select Category")
                .setItems(names, (dialog, which) -> {
                    if (which == 0) {
                        currentRootCategoryId = "all";
                        tvSelectedTableCategory.setText("All Category");
                        viewModel.loadTableData("all");
                    } else {
                        CategoryEntity selected = categories.get(which - 1);
                        currentRootCategoryId = selected.getCategoryId();
                        tvSelectedTableCategory.setText(selected.getName());
                        viewModel.loadTableData(selected.getCategoryId());
                    }
                })
                .show();
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
        
        viewModel.getTableColumns().observe(this, this::renderTableHeader);
        viewModel.getTableColumnGroups().observe(this, this::renderTableGroups);
        viewModel.getTableRows().observe(this, rows -> tableAdapter.setRows(rows));

        viewModel.getHeatmapColumns().observe(this, cols -> {
            heatmapAdapter.setColumns(cols);
            renderHeatmapHeader(cols);
        });
        viewModel.getHeatmapRows().observe(this, rows -> heatmapAdapter.setRows(rows));

        viewModel.getAvailableTags().observe(this, tags -> {
            updateTagChips(tags, viewModel.getSelectedTagIds().getValue());
        });
        viewModel.getSelectedTagIds().observe(this, selectedIds -> {
            updateTagChips(viewModel.getAvailableTags().getValue(), selectedIds);
        });

        viewModel.getCategoryTrends().observe(this, this::bindCategoryTrends);

        viewModel.getTableExpandable().observe(this, expanded -> {
            tableExpandableContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            tvExpandTable.setText(expanded ? "Tap to collapse" : "Tap to expand table");
        });
        
        viewModel.getRootCategories().observe(this, roots -> {
            if (roots != null && !roots.isEmpty()) {
                if (currentRootCategoryId == null) {
                    currentRootCategoryId = roots.get(0).getCategoryId();
                }
                
                // Update dropdown text based on current root category
                if ("all".equals(currentRootCategoryId)) {
                    tvSelectedTableCategory.setText("All Category");
                } else {
                    for (CategoryEntity root : roots) {
                        if (root.getCategoryId().equals(currentRootCategoryId)) {
                            tvSelectedTableCategory.setText(root.getName());
                            break;
                        }
                    }
                }

                if (tvSelectedTableCategory.getText().equals("Category")) {
                    tvSelectedTableCategory.setText(roots.get(0).getName());
                }
            }
        });
    }

    private void renderTableHeader(List<TableColumn> columns) {
        tableAdapter.setColumns(columns);
        tableHeaderContainer.removeAllViews();
        int cellWidth = (int) (80 * getResources().getDisplayMetrics().density);

        for (TableColumn col : columns) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(cellWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(24, 16, 24, 16);
            tv.setText(col.getTitle());
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(12);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setGravity(android.view.Gravity.CENTER);
            tableHeaderContainer.addView(tv);
        }
    }

    private void renderTableGroups(List<AnalyticsViewModel.CategoryGroup> groups) {
        tableGroupHeaderContainer.removeAllViews();
        if (groups == null || groups.isEmpty()) {
            tableGroupHeaderContainer.setVisibility(View.GONE);
            return;
        }

        tableGroupHeaderContainer.setVisibility(View.VISIBLE);
        int cellWidth = (int) (80 * getResources().getDisplayMetrics().density);

        for (AnalyticsViewModel.CategoryGroup group : groups) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(cellWidth * group.span, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(24, 16, 24, 16);
            tv.setText(group.name);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(12);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setGravity(android.view.Gravity.CENTER);
            
            if (group.span > 1 && !group.name.isEmpty()) {
                tv.setBackgroundResource(R.drawable.bg_table_group_header);
            }
            
            tableGroupHeaderContainer.addView(tv);
        }
    }

    private void bindCategoryTrends(List<CategoryTrendData> trends) {
        if (categoryTrendChartsContainer == null) return;
        categoryTrendChartsContainer.removeAllViews();
        if (trends == null || trends.isEmpty()) {
            return;
        }

        PeriodType type = viewModel.getPeriodType().getValue();

        for (CategoryTrendData data : trends) {
            // Title with Icon
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            titleParams.setMargins(0, 32, 0, 8);
            tv.setLayoutParams(titleParams);
            
            String icon = data.getCategoryIcon() != null ? data.getCategoryIcon() : "📊";
            tv.setText(icon + " " + data.getCategoryName() + " Trend");
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(14);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            categoryTrendChartsContainer.addView(tv);

            if (type == PeriodType.MONTH) {
                CombinedChart chart = new CombinedChart(this);
                chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (220 * getResources().getDisplayMetrics().density)));
                setupCombinedChart(chart, data);
                categoryTrendChartsContainer.addView(chart);
                chart.animateY(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
            } else {
                LineChart chart = new LineChart(this);
                chart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (200 * getResources().getDisplayMetrics().density)));
                setupYearlyLineChart(chart, data);
                categoryTrendChartsContainer.addView(chart);
                chart.animateX(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
            }
        }
    }

    private void setupCombinedChart(CombinedChart chart, CategoryTrendData data) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setHighlightFullBarEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.border_color));
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        leftAxis.setAxisMinimum(0f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        xAxis.setGranularity(1f);

        CombinedData combinedData = new CombinedData();
        
        // Bar Data: Historical totals (3 months)
        List<BarEntry> barEntries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < data.getHistoricalTotals().size(); i++) {
            CategoryTrendData.MonthlyTotal total = data.getHistoricalTotals().get(i);
            barEntries.add(new BarEntry(i, (float) total.getAmount()));
            labels.add(total.getLabel());
        }
        BarDataSet barSet = new BarDataSet(barEntries, "Monthly Total");
        barSet.setColor(ContextCompat.getColor(this, R.color.elevated_surface));
        barSet.setDrawValues(true);
        barSet.setValueTextColor(Color.WHITE);
        barSet.setValueTextSize(10f);
        barSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value > 0 ? String.format(Locale.getDefault(), "%.0f", value) : "";
            }
        });
        BarData barData = new BarData(barSet);
        barData.setBarWidth(0.5f);
        combinedData.setData(barData);

        // Line Data: Daily Trend for the current month (overlaid on the 3rd bar)
        List<Entry> lineEntries = new ArrayList<>();
        List<DailyExpense> daily = data.getDailyTrend();
        
        if (daily != null && !daily.isEmpty()) {
            // Map the current month range to X axis range corresponding to the 3rd bar (index 2)
            // Bar centers are at 0, 1, 2. Bar width is 0.5.
            // We'll span the line from 1.6 to 2.4 to stay within the bar's area
            float startX = 1.6f;
            float endX = 2.4f;
            for (int i = 0; i < daily.size(); i++) {
                float progress = (float) i / daily.size();
                float x = startX + (progress * (endX - startX));
                lineEntries.add(new Entry(x, (float) daily.get(i).getAmount()));
            }
        }
        
        LineDataSet lineSet = new LineDataSet(lineEntries, "Daily Trend");
        try {
            lineSet.setColor(Color.parseColor(data.getCategoryColor()));
        } catch (Exception e) {
            lineSet.setColor(ContextCompat.getColor(this, R.color.accent_purple));
        }
        lineSet.setLineWidth(2.5f);
        lineSet.setDrawCircles(false);
        lineSet.setMode(LineDataSet.Mode.LINEAR);
        lineSet.setDrawValues(false);
        
        LineData lineData = new LineData(lineSet);
        combinedData.setData(lineData);

        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(2.5f);

        chart.setData(combinedData);
        chart.setDrawOrder(new CombinedChart.DrawOrder[]{
                CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE
        });
        
        float yMax = Math.max(combinedData.getYMax(), 10f);
        chart.getAxisLeft().setAxisMaximum(yMax * 1.1f);
        chart.getAxisLeft().setAxisMinimum(0f);

        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void setupYearlyLineChart(LineChart chart, CategoryTrendData data) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        xAxis.setLabelCount(12);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.border_color));
        leftAxis.setTextColor(ContextCompat.getColor(this, R.color.text_muted));
        leftAxis.setAxisMinimum(0f);

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < data.getYearlyTrend().size(); i++) {
            CategoryTrendData.MonthlyTotal total = data.getYearlyTrend().get(i);
            entries.add(new Entry(i, (float) total.getAmount()));
            labels.add(total.getLabel());
        }

        LineDataSet set = new LineDataSet(entries, "");
        try {
            set.setColor(Color.parseColor(data.getCategoryColor()));
        } catch (Exception e) {
            set.setColor(ContextCompat.getColor(this, R.color.accent_purple));
        }
        set.setLineWidth(2.5f);
        set.setDrawCircles(true);
        set.setCircleRadius(3f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillAlpha(40);
        try {
            set.setFillColor(Color.parseColor(data.getCategoryColor()));
        } catch (Exception e) {
            set.setFillColor(ContextCompat.getColor(this, R.color.accent_purple));
        }

        chart.setData(new LineData(set));
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.animateX(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
        chart.invalidate();
    }

    private void renderHeatmapHeader(List<TableColumn> columns) {
        heatmapHeaderContainer.removeAllViews();
        int cellWidth = (int) (80 * getResources().getDisplayMetrics().density);

        for (TableColumn col : columns) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(cellWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv.setPadding(24, 16, 24, 16);
            tv.setText(col.getTitle());
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(11);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setGravity(android.view.Gravity.CENTER);
            heatmapHeaderContainer.addView(tv);
        }
    }

    private void updateTagChips(List<com.example.pp.features.category.model.Tag> tags, List<String> selectedIds) {
        if (tagChipGroup == null) return;
        tagChipGroup.removeAllViews();
        if (tags == null) return;

        for (com.example.pp.features.category.model.Tag tag : tags) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(tag.getName());
            chip.setCheckable(false); // We'll handle selection state via background color
            boolean isChecked = selectedIds != null && selectedIds.contains(tag.getId());
            
            // Custom styling to match theme
            if (isChecked) {
                chip.setChipBackgroundColorResource(R.color.accent_purple);
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setChipBackgroundColorResource(R.color.elevated_surface);
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
            chip.setChipStrokeWidth(0);
            
            chip.setOnClickListener(v -> viewModel.toggleTag(tag.getId()));
            tagChipGroup.addView(chip);
        }
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

    private void showTransactionListBottomSheet(long timestamp) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.RoundedBottomSheetDialog);
        View sheet = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_drill_down, null);
        TextView tvTitle = sheet.findViewById(R.id.tvDrillDownTitle);
        TextView tvSubtitle = sheet.findViewById(R.id.tvDrillDownSubtitle);
        RecyclerView rvTransactions = sheet.findViewById(R.id.rvDrillDown);

        PeriodType type = viewModel.getPeriodType().getValue();
        SimpleDateFormat sdf = (type == PeriodType.YEAR)
                ? new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                : new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        
        tvTitle.setText("Transactions");
        tvSubtitle.setText("For " + sdf.format(timestamp));

        TransactionAdapter adapter = new TransactionAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        viewModel.getPeriodTransactions().observe(this, transactions -> {
            if (transactions != null) adapter.submitList(transactions);
        });

        dialog.setContentView(sheet);
        dialog.show();
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
            tvHighestCategoryName.setTextColor(ContextCompat.getColor(this, R.color.electric_blue));
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

        // Group top 5 and 'Others'
        List<CategoryExpense> topExpenses = new ArrayList<>(expenses);
        topExpenses.sort((a, b) -> Double.compare(b.getAmount(), a.getAmount()));

        List<CategoryExpense> chartExpenses = new ArrayList<>();
        double othersSum = 0;
        for (int i = 0; i < topExpenses.size(); i++) {
            if (i < 5) {
                chartExpenses.add(topExpenses.get(i));
            } else {
                othersSum += topExpenses.get(i).getAmount();
            }
        }
        if (othersSum > 0) {
            String mutedColor = String.format("#%06X", (0xFFFFFF & ContextCompat.getColor(this, R.color.text_muted)));
            chartExpenses.add(new CategoryExpense("others", null, "Others", "📁", mutedColor, othersSum, (othersSum/total)*100));
        }

        for (CategoryExpense ce : chartExpenses) {
            float val = (float) ce.getAmount();
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
        
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.5f);
        dataSet.setValueLinePart2Length(0.6f);
        dataSet.setValueLineColor(ContextCompat.getColor(this, R.color.text_muted));
        dataSet.setValueLineWidth(1f);
        dataSet.setValueLineVariableLength(true);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return ""; // Only show category names
            }
        });
        
        donutChart.setData(data);
        donutChart.setUsePercentValues(false);
        donutChart.setDrawEntryLabels(true);
        donutChart.setEntryLabelColor(Color.WHITE);
        donutChart.setEntryLabelTextSize(11f);
        donutChart.setExtraOffsets(30f, 10f, 30f, 10f); // More space for labels
        
        donutChart.setCenterText(String.format(Locale.getDefault(), "\u20B9 %,.0f\nTotal\n%s", total, viewModel.getPeriodLabel().getValue() != null ? viewModel.getPeriodLabel().getValue() : ""));
        donutChart.animateY(1400, com.github.mikephil.charting.animation.Easing.EaseInOutQuad);
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
        set.setColor(ContextCompat.getColor(this, R.color.accent_purple));
        set.setLineWidth(1.5f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        sparklineChart.setData(new LineData(set));
        sparklineChart.animateX(500);
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
        set.setColor(ContextCompat.getColor(this, R.color.accent_purple));
        set.setLineWidth(2f);
        set.setDrawCircles(true);
        set.setCircleColor(ContextCompat.getColor(this, R.color.accent_pink));
        set.setCircleRadius(3f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setFillColor(ContextCompat.getColor(this, R.color.chart_trend_fill));
        set.setDrawFilled(true);
        set.setHighlightLineWidth(1.5f);
        set.setHighLightColor(ContextCompat.getColor(this, R.color.highlight_yellow));

        trendChart.setData(new LineData(set));
        trendChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));

        TrendMarkerView marker = (TrendMarkerView) trendChart.getMarker();
        if (marker != null) {
            marker.setDateTimestamps(timestamps);
            marker.setDatePattern(type == PeriodType.YEAR ? "d MMM" : "d MMM");
        }
        trendChart.animateX(1000, com.github.mikephil.charting.animation.Easing.EaseInOutQuart);
        trendChart.invalidate();
    }
}

