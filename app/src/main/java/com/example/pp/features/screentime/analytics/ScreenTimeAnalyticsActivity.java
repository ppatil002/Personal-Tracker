package com.example.pp.features.screentime.analytics;

import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.core.enums.PeriodType;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.features.analytics.chart.TimeMarkerView;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScreenTimeAnalyticsActivity extends AppCompatActivity {

    private ScreenTimeAnalyticsViewModel viewModel;
    private BarChart usageChart;
    private LineChart usageLineChart;
    private RecyclerView rvApps;
    private AppAdapter adapter;
    private TextView tvPeriodLabel, tvChartLabel, tvPeriodType;
    private TextView tvAvgValue, tvPercentChange, tvTargetStatus;
    private View targetStatsContainer;
    private View periodDropdown, btnCalendar;
    private TabLayout tabLayoutBreakdown;

    private static final long TARGET_4H = 4 * 3600 * 1000L;
    private static final long WARNING_6H = 6 * 3600 * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_time_analytics);

        viewModel = new ViewModelProvider(this).get(ScreenTimeAnalyticsViewModel.class);

        initViews();
        setupCharts();
        setupRecyclerView();
        observeViewModel();
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        usageChart = findViewById(R.id.usageChart);
        usageLineChart = findViewById(R.id.usageLineChart);
        rvApps = findViewById(R.id.rvApps);
        tvPeriodLabel = findViewById(R.id.tvPeriodLabel);
        tvChartLabel = findViewById(R.id.tvChartLabel);
        tvPeriodType = findViewById(R.id.tvPeriodType);
        tvAvgValue = findViewById(R.id.tvAvgValue);
        tvPercentChange = findViewById(R.id.tvPercentChange);
        tvTargetStatus = findViewById(R.id.tvTargetStatus);
        targetStatsContainer = findViewById(R.id.targetStatsContainer);
        periodDropdown = findViewById(R.id.periodDropdown);
        btnCalendar = findViewById(R.id.btnCalendar);
        tabLayoutBreakdown = findViewById(R.id.tabLayoutBreakdown);

        findViewById(R.id.btnPrev).setOnClickListener(v -> viewModel.navigatePeriod(-1));
        findViewById(R.id.btnNext).setOnClickListener(v -> viewModel.navigatePeriod(1));

        periodDropdown.setOnClickListener(v -> showPeriodPopup());
        btnCalendar.setOnClickListener(v -> showDatePicker());

        tabLayoutBreakdown.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: viewModel.setBreakdownType(ScreenTimeAnalyticsViewModel.BreakdownType.SCREEN_TIME); break;
                    case 1: viewModel.setBreakdownType(ScreenTimeAnalyticsViewModel.BreakdownType.TIMES_OPENED); break;
                    case 2: viewModel.setBreakdownType(ScreenTimeAnalyticsViewModel.BreakdownType.NOTIFICATIONS); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupCharts() {
        // Bar Chart Setup
        usageChart.getDescription().setEnabled(false);
        usageChart.setDrawGridBackground(false);
        usageChart.setDrawBarShadow(false);
        usageChart.getLegend().setEnabled(false);

        XAxis xAxis = usageChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGranularity(1f);

        YAxis leftAxis = usageChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(this, R.color.stroke_color));
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (viewModel.getBreakdownType().getValue() == ScreenTimeAnalyticsViewModel.BreakdownType.SCREEN_TIME) {
                    return formatMillis((long) value);
                }
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        usageChart.getAxisRight().setEnabled(false);

        // Line Chart Setup
        usageLineChart.getDescription().setEnabled(false);
        usageLineChart.setDrawGridBackground(false);
        usageLineChart.getLegend().setEnabled(false);
        XAxis lxAxis = usageLineChart.getXAxis();
        lxAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        lxAxis.setDrawGridLines(false);
        lxAxis.setTextColor(Color.WHITE);
        lxAxis.setGranularity(1f);
        YAxis lLeftAxis = usageLineChart.getAxisLeft();
        lLeftAxis.setDrawGridLines(true);
        lLeftAxis.setGridColor(ContextCompat.getColor(this, R.color.stroke_color));
        lLeftAxis.setTextColor(Color.WHITE);
        lLeftAxis.setAxisMinimum(0f);
        usageLineChart.getAxisRight().setEnabled(false);
    }

    private void observeViewModel() {
        viewModel.getPeriodLabel().observe(this, tvPeriodLabel::setText);
        viewModel.getPeriodType().observe(this, type -> {
            String typeStr = type.name().substring(0, 1).toUpperCase() + type.name().substring(1).toLowerCase();
            tvPeriodType.setText(typeStr);
            updateChartLabel();
        });

        viewModel.getBreakdownType().observe(this, type -> {
            boolean isScreenTime = type == ScreenTimeAnalyticsViewModel.BreakdownType.SCREEN_TIME;
            usageChart.setVisibility(isScreenTime ? View.VISIBLE : View.GONE);
            usageLineChart.setVisibility(isScreenTime ? View.GONE : View.VISIBLE);
            updateChartLabel();
            
            List<ScreenTimeDao.HourlyUsageSummary> data = viewModel.getHourlyUsage().getValue();
            if (data != null) renderChart(data);
        });

        viewModel.getStats().observe(this, this::bindStats);
        viewModel.getHourlyUsage().observe(this, this::renderChart);
        viewModel.getAppUsage().observe(this, this::updateAppList);
    }

    private void updateChartLabel() {
        PeriodType p = viewModel.getPeriodType().getValue();
        ScreenTimeAnalyticsViewModel.BreakdownType b = viewModel.getBreakdownType().getValue();
        String periodStr = p == PeriodType.DAY ? "Hourly" : "Daily";
        String metricStr = b == ScreenTimeAnalyticsViewModel.BreakdownType.SCREEN_TIME ? "Screen Time" : (b == ScreenTimeAnalyticsViewModel.BreakdownType.TIMES_OPENED ? "Opens" : "Notifications");
        tvChartLabel.setText(periodStr + " " + metricStr);
    }

    private void bindStats(ScreenTimeAnalyticsViewModel.ScreenTimeStats s) {
        if (s == null) return;
        
        ScreenTimeAnalyticsViewModel.BreakdownType bType = viewModel.getBreakdownType().getValue();
        if (bType == ScreenTimeAnalyticsViewModel.BreakdownType.SCREEN_TIME) {
            tvAvgValue.setText(formatMillis((long) s.avgValue));
        } else {
            tvAvgValue.setText(String.format(Locale.getDefault(), "%.1f", s.avgValue));
        }

        String changeStr = String.format(Locale.getDefault(), "%s%.1f%%", s.percentChange >= 0 ? "+" : "", s.percentChange);
        tvPercentChange.setText(changeStr);
        tvPercentChange.setTextColor(ContextCompat.getColor(this, s.percentChange > 0 ? R.color.error_red : R.color.emerald_green));

        if (bType == ScreenTimeAnalyticsViewModel.BreakdownType.SCREEN_TIME && viewModel.getPeriodType().getValue() != PeriodType.DAY) {
            targetStatsContainer.setVisibility(View.VISIBLE);
            tvTargetStatus.setText(s.targetMetCount + " / " + (s.targetMetCount + s.targetNotMetCount));
        } else {
            targetStatsContainer.setVisibility(View.GONE);
        }
    }

    private void renderChart(List<ScreenTimeDao.HourlyUsageSummary> data) {
        if (data == null) return;
        ScreenTimeAnalyticsViewModel.BreakdownType bType = viewModel.getBreakdownType().getValue();
        if (bType == null) bType = ScreenTimeAnalyticsViewModel.BreakdownType.SCREEN_TIME;

        if (bType == ScreenTimeAnalyticsViewModel.BreakdownType.SCREEN_TIME) {
            renderBarChart(data);
        } else {
            renderLineChart(data);
        }
    }

    private void renderBarChart(List<ScreenTimeDao.HourlyUsageSummary> data) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        
        PeriodType type = viewModel.getPeriodType().getValue();
        long start = getRangeStart();
        long end = getRangeEnd();

        if (type == PeriodType.DAY) {
            long cumulative = 0;
            for (int i = 0; i < 24; i++) {
                long duration = 0;
                for (ScreenTimeDao.HourlyUsageSummary h : data) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(h.hourStartTimestamp);
                    if (c.get(Calendar.HOUR_OF_DAY) == i) {
                        duration = h.totalDuration;
                        break;
                    }
                }
                entries.add(new BarEntry(i, duration));
                labels.add(i + "h");
                cumulative += duration;
                colors.add(ContextCompat.getColor(this, cumulative > TARGET_4H ? R.color.error_red : R.color.electric_blue));
            }
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(start);
            int i = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
            while (c.getTimeInMillis() < end) {
                long dStart = c.getTimeInMillis();
                c.add(Calendar.DAY_OF_YEAR, 1);
                long dEnd = c.getTimeInMillis();
                long duration = 0;
                for (ScreenTimeDao.HourlyUsageSummary h : data) {
                    if (h.hourStartTimestamp >= dStart && h.hourStartTimestamp < dEnd) {
                        duration += h.totalDuration;
                    }
                }
                entries.add(new BarEntry(i, duration));
                labels.add(sdf.format(dStart));
                if (duration > WARNING_6H) colors.add(ContextCompat.getColor(this, R.color.error_red));
                else if (duration > TARGET_4H) colors.add(ContextCompat.getColor(this, R.color.warning_orange));
                else colors.add(ContextCompat.getColor(this, R.color.electric_blue));
                i++;
            }
            
            // Add Target Line
            YAxis leftAxis = usageChart.getAxisLeft();
            leftAxis.removeAllLimitLines();
            LimitLine ll = new LimitLine(TARGET_4H, "Target 4h");
            ll.setLineColor(Color.WHITE);
            ll.setLineWidth(1f);
            ll.enableDashedLine(10f, 10f, 0f);
            ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
            ll.setTextColor(Color.WHITE);
            leftAxis.addLimitLine(ll);
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setColors(colors);
        set.setDrawValues(false);
        usageChart.setData(new BarData(set));
        usageChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        
        TimeMarkerView marker = new TimeMarkerView(this);
        marker.setLabels(labels.toArray(new String[0]));
        usageChart.setMarker(marker);
        usageChart.invalidate();
        usageChart.animateY(1000);
    }

    private void renderLineChart(List<ScreenTimeDao.HourlyUsageSummary> data) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        ScreenTimeAnalyticsViewModel.BreakdownType bType = viewModel.getBreakdownType().getValue();
        PeriodType type = viewModel.getPeriodType().getValue();
        long start = getRangeStart();
        long end = getRangeEnd();

        if (type == PeriodType.DAY) {
            for (int i = 0; i < 24; i++) {
                float val = 0;
                for (ScreenTimeDao.HourlyUsageSummary h : data) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(h.hourStartTimestamp);
                    if (c.get(Calendar.HOUR_OF_DAY) == i) {
                        val = (bType == ScreenTimeAnalyticsViewModel.BreakdownType.TIMES_OPENED) ? h.totalOpens : h.totalNotifications;
                        break;
                    }
                }
                entries.add(new Entry(i, val));
                labels.add(i + "h");
            }
        } else {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(start);
            int i = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
            while (c.getTimeInMillis() < end) {
                long dStart = c.getTimeInMillis();
                c.add(Calendar.DAY_OF_YEAR, 1);
                long dEnd = c.getTimeInMillis();
                float val = 0;
                for (ScreenTimeDao.HourlyUsageSummary h : data) {
                    if (h.hourStartTimestamp >= dStart && h.hourStartTimestamp < dEnd) {
                        val += (bType == ScreenTimeAnalyticsViewModel.BreakdownType.TIMES_OPENED) ? h.totalOpens : h.totalNotifications;
                    }
                }
                entries.add(new Entry(i, val));
                labels.add(sdf.format(dStart));
                i++;
            }
        }

        LineDataSet set = new LineDataSet(entries, "");
        set.setColor(ContextCompat.getColor(this, R.color.accent_purple));
        set.setLineWidth(2.5f);
        set.setDrawCircles(true);
        set.setCircleColor(ContextCompat.getColor(this, R.color.accent_purple));
        set.setCircleRadius(4f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillAlpha(40);
        set.setFillColor(ContextCompat.getColor(this, R.color.accent_purple));
        set.setDrawValues(false);

        usageLineChart.setData(new LineData(set));
        usageLineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        usageLineChart.invalidate();
        usageLineChart.animateY(1000);
    }

    private void updateAppList(List<ScreenTimeDao.AppUsageSummary> apps) {
        long total = 0;
        if (apps != null) {
            for (ScreenTimeDao.AppUsageSummary app : apps) total += app.totalDuration;
        }
        adapter.setData(apps != null ? apps : new ArrayList<>(), total);
    }

    private void setupRecyclerView() {
        rvApps.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppAdapter();
        rvApps.setAdapter(adapter);
        adapter.setOnItemClickListener(app -> {
            viewModel.setSelectedPackageName(app.packageName);
            // Re-render chart
            List<ScreenTimeDao.HourlyUsageSummary> current = viewModel.getHourlyUsage().getValue();
            if (current != null) renderChart(current);
        });
    }

    private void showPeriodPopup() {
        View popupView = LayoutInflater.from(this).inflate(R.layout.popup_period_type_screentime, null);
        PopupWindow popup = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setElevation(8f);
        popup.showAsDropDown(periodDropdown);

        popupView.findViewById(R.id.tvOptionDay).setOnClickListener(v -> {
            viewModel.setPeriodType(PeriodType.DAY);
            popup.dismiss();
        });
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

    // Helper methods to match ViewModel's range
    private long getRangeStart() {
        Long dateVal = viewModel.getSelectedDate().getValue();
        long date = dateVal != null ? dateVal : System.currentTimeMillis();
        PeriodType type = viewModel.getPeriodType().getValue();
        if (type == null) type = PeriodType.DAY;

        switch (type) {
            case WEEK: return com.example.pp.utils.DateUtils.getStartOfWeek(date);
            case MONTH: return com.example.pp.utils.DateUtils.getStartOfMonth(date);
            default: return date;
        }
    }

    private long getRangeEnd() {
        PeriodType type = viewModel.getPeriodType().getValue();
        if (type == null) type = PeriodType.DAY;
        
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getRangeStart());
        switch (type) {
            case DAY: cal.add(Calendar.DAY_OF_YEAR, 1); break;
            case WEEK: cal.add(Calendar.WEEK_OF_YEAR, 1); break;
            case MONTH: cal.add(Calendar.MONTH, 1); break;
        }
        return cal.getTimeInMillis();
    }

    private interface OnAppItemClickListener { void onItemClick(ScreenTimeDao.AppUsageSummary app); }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
        private List<ScreenTimeDao.AppUsageSummary> items = new ArrayList<>();
        private long totalDuration = 0;
        private OnAppItemClickListener listener;

        public void setOnItemClickListener(OnAppItemClickListener l) { this.listener = l; }

        public void setData(List<ScreenTimeDao.AppUsageSummary> items, long total) {
            this.items = items;
            this.totalDuration = total;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_screen_time_app, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ScreenTimeDao.AppUsageSummary item = items.get(position);
            holder.tvPackageName.setText(item.packageName);
            
            ScreenTimeAnalyticsViewModel.BreakdownType bType = viewModel.getBreakdownType().getValue();
            if (bType == ScreenTimeAnalyticsViewModel.BreakdownType.SCREEN_TIME) {
                holder.tvDuration.setText(formatMillis(item.totalDuration));
                float percent = totalDuration > 0 ? (float) item.totalDuration / totalDuration * 100 : 0;
                holder.tvPercent.setText(String.format(Locale.getDefault(), "%.1f%%", percent));
            } else if (bType == ScreenTimeAnalyticsViewModel.BreakdownType.TIMES_OPENED) {
                holder.tvDuration.setText(item.totalOpens + " opens");
                holder.tvPercent.setText("");
            } else {
                holder.tvDuration.setText(item.totalNotifications + " notifications");
                holder.tvPercent.setText("");
            }

            PackageManager pm = getPackageManager();
            try {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(item.packageName, 0);
                holder.tvAppName.setText(pm.getApplicationLabel(ai));
                holder.ivAppIcon.setImageDrawable(ai.loadIcon(pm));
            } catch (PackageManager.NameNotFoundException e) {
                holder.tvAppName.setText(item.packageName);
                holder.ivAppIcon.setImageResource(R.drawable.ic_placeholder);
            }
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAppIcon;
            TextView tvAppName, tvPackageName, tvDuration, tvPercent;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
                tvAppName = itemView.findViewById(R.id.tvAppName);
                tvPackageName = itemView.findViewById(R.id.tvPackageName);
                tvDuration = itemView.findViewById(R.id.tvDuration);
                tvPercent = itemView.findViewById(R.id.tvPercent);
            }
        }
    }

    private String formatMillis(long millis) {
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }
}
