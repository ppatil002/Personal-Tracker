package com.example.pp.features.screentime.analytics;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.core.enums.PeriodType;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.data.repository.ScreenTimeRepository;
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

public class ScreenTimeAnalyticsActivity extends AppCompatActivity {

    private ScreenTimeRepository repository;
    private BarChart usageChart;
    private RecyclerView rvApps;
    private AppAdapter adapter;
    private TextView tvChartLabel;

    private long currentStart;
    private long currentEnd;
    private PeriodType currentPeriod = PeriodType.DAY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_time_analytics);

        repository = ((PPApplication) getApplication()).getScreenTimeRepository();

        initViews();
        setupChart();
        setupRecyclerView();
        
        updateRange(PeriodType.DAY);
    }

    private void initViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        usageChart = findViewById(R.id.usageChart);
        rvApps = findViewById(R.id.rvApps);
        tvChartLabel = findViewById(R.id.tvChartLabel);
        MaterialButtonToggleGroup togglePeriod = findViewById(R.id.togglePeriod);

        togglePeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnDay) updateRange(PeriodType.DAY);
                else if (checkedId == R.id.btnWeek) updateRange(PeriodType.WEEK);
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
        
        currentEnd = System.currentTimeMillis() + 3600000L; // Current hour end
        
        if (period == PeriodType.DAY) {
            currentStart = cal.getTimeInMillis();
            tvChartLabel.setText("Hourly Breakdown (Today)");
        } else if (period == PeriodType.WEEK) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            currentStart = cal.getTimeInMillis();
            tvChartLabel.setText("Daily Breakdown (This Week)");
        } else if (period == PeriodType.MONTH) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            currentStart = cal.getTimeInMillis();
            tvChartLabel.setText("Daily Breakdown (This Month)");
        }

        observeData();
    }

    private LiveData<List<ScreenTimeDao.HourlyUsageSummary>> currentHourlyData;
    private LiveData<List<ScreenTimeDao.AppUsageSummary>> currentAppData;

    private void observeData() {
        if (currentHourlyData != null) currentHourlyData.removeObservers(this);
        if (currentAppData != null) currentAppData.removeObservers(this);

        currentHourlyData = repository.getHourlyUsage(currentStart, currentEnd);
        currentAppData = repository.getAppUsageRange(currentStart, currentEnd);

        currentHourlyData.observe(this, this::updateChart);
        currentAppData.observe(this, this::updateAppList);
    }

    private void setupChart() {
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
                return formatMillis((long) value);
            }
        });

        usageChart.getAxisRight().setEnabled(false);
    }

    private void updateChart(List<ScreenTimeDao.HourlyUsageSummary> data) {
        if (data == null || data.isEmpty()) {
            android.util.Log.d("ScreenTimeAnalytics PP", "No hourly data received for chart.");
            usageChart.clear();
            return;
        }
        android.util.Log.d("ScreenTimeAnalytics PP", "Updating chart with " + data.size() + " summaries.");

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        if (currentPeriod == PeriodType.DAY) {
            // Hour-wise (0-23)
            for (int i = 0; i < 24; i++) {
                long duration = 0;
                for (ScreenTimeDao.HourlyUsageSummary summary : data) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(summary.hourStartTimestamp);
                    if (c.get(Calendar.HOUR_OF_DAY) == i) {
                        duration = summary.totalDuration;
                        break;
                    }
                }
                entries.add(new BarEntry(i, duration));
                labels.add(i + "h");
            }
        } else {
            // Day-wise
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(currentStart);
            int i = 0;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
            while (c.getTimeInMillis() < currentEnd) {
                long dayStart = c.getTimeInMillis();
                c.add(Calendar.DAY_OF_YEAR, 1);
                long dayEnd = c.getTimeInMillis();
                
                long duration = 0;
                for (ScreenTimeDao.HourlyUsageSummary summary : data) {
                    if (summary.hourStartTimestamp >= dayStart && summary.hourStartTimestamp < dayEnd) {
                        duration += summary.totalDuration;
                    }
                }
                entries.add(new BarEntry(i, duration));
                labels.add(sdf.format(dayStart));
                i++;
            }
        }

        BarDataSet set = new BarDataSet(entries, "Usage");
        set.setColor(ContextCompat.getColor(this, R.color.electric_blue));
        set.setDrawValues(false);

        BarData barData = new BarData(set);
        usageChart.setData(barData);
        usageChart.getXAxis().setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels));
        usageChart.invalidate();
    }

    private void setupRecyclerView() {
        rvApps.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppAdapter();
        rvApps.setAdapter(adapter);
    }

    private void updateAppList(List<ScreenTimeDao.AppUsageSummary> apps) {
        if (apps == null || apps.isEmpty()) {
            android.util.Log.d("ScreenTimeAnalytics PP", "No app usage data received for list.");
            adapter.setData(new ArrayList<>(), 0);
            return;
        }
        android.util.Log.d("ScreenTimeAnalytics PP", "Updating app list with " + apps.size() + " apps.");
        long total = 0;
        for (ScreenTimeDao.AppUsageSummary app : apps) total += app.totalDuration;
        adapter.setData(apps, total);
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {
        private List<ScreenTimeDao.AppUsageSummary> items = new ArrayList<>();
        private long totalDuration = 0;

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
            holder.tvDuration.setText(formatMillis(item.totalDuration));
            
            float percent = totalDuration > 0 ? (float) item.totalDuration / totalDuration * 100 : 0;
            holder.tvPercent.setText(String.format(Locale.getDefault(), "%.1f%%", percent));

            PackageManager pm = getPackageManager();
            try {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(item.packageName, 0);
                holder.tvAppName.setText(pm.getApplicationLabel(ai));
                holder.ivAppIcon.setImageDrawable(ai.loadIcon(pm));
            } catch (PackageManager.NameNotFoundException e) {
                holder.tvAppName.setText(item.packageName);
                holder.ivAppIcon.setImageResource(R.drawable.ic_placeholder);
            }
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
