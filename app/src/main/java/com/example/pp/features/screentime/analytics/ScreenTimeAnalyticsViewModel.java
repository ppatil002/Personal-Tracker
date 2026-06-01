package com.example.pp.features.screentime.analytics;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MediatorLiveData;
import com.example.pp.PPApplication;
import com.example.pp.core.enums.PeriodType;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.ScreenTimeDao;
import com.example.pp.data.repository.ScreenTimeRepository;
import com.example.pp.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ScreenTimeAnalyticsViewModel extends AndroidViewModel {

    public enum BreakdownType {
        SCREEN_TIME, TIMES_OPENED, NOTIFICATIONS
    }

    private final ScreenTimeRepository repository;

    private final MutableLiveData<PeriodType> periodType = new MutableLiveData<>(PeriodType.DAY);
    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>(DateUtils.getStartOfDay(System.currentTimeMillis()));
    private final MutableLiveData<BreakdownType> breakdownType = new MutableLiveData<>(BreakdownType.SCREEN_TIME);
    private final MutableLiveData<String> selectedPackageName = new MutableLiveData<>(null);

    private final MutableLiveData<String> periodLabel = new MutableLiveData<>("");
    private final MutableLiveData<ScreenTimeStats> stats = new MutableLiveData<>();
    
    private final MediatorLiveData<List<ScreenTimeDao.HourlyUsageSummary>> hourlyUsage = new MediatorLiveData<>();
    private final MediatorLiveData<List<ScreenTimeDao.AppUsageSummary>> appUsage = new MediatorLiveData<>();

    public ScreenTimeAnalyticsViewModel(@NonNull Application application) {
        super(application);
        this.repository = ((PPApplication) application).getScreenTimeRepository();
        updatePeriodLabel();

        hourlyUsage.addSource(periodType, t -> refreshHourlyUsage());
        hourlyUsage.addSource(selectedDate, t -> refreshHourlyUsage());
        hourlyUsage.addSource(selectedPackageName, t -> refreshHourlyUsage());

        appUsage.addSource(periodType, t -> refreshAppUsage());
        appUsage.addSource(selectedDate, t -> refreshAppUsage());
    }

    private LiveData<List<ScreenTimeDao.HourlyUsageSummary>> currentHourlySource;
    private void refreshHourlyUsage() {
        if (currentHourlySource != null) hourlyUsage.removeSource(currentHourlySource);
        currentHourlySource = repository.getHourlyUsage(getRangeStart(), getRangeEnd(), selectedPackageName.getValue());
        hourlyUsage.addSource(currentHourlySource, hourlyUsage::setValue);
    }

    private LiveData<List<ScreenTimeDao.AppUsageSummary>> currentAppSource;
    private void refreshAppUsage() {
        if (currentAppSource != null) appUsage.removeSource(currentAppSource);
        currentAppSource = repository.getAppUsageRange(getRangeStart(), getRangeEnd());
        appUsage.addSource(currentAppSource, appUsage::setValue);
    }

    public LiveData<PeriodType> getPeriodType() { return periodType; }
    public LiveData<Long> getSelectedDate() { return selectedDate; }
    public LiveData<BreakdownType> getBreakdownType() { return breakdownType; }
    public LiveData<String> getSelectedPackageName() { return selectedPackageName; }
    public LiveData<String> getPeriodLabel() { return periodLabel; }
    public LiveData<ScreenTimeStats> getStats() { return stats; }
    public LiveData<List<ScreenTimeDao.HourlyUsageSummary>> getHourlyUsage() { return hourlyUsage; }
    public LiveData<List<ScreenTimeDao.AppUsageSummary>> getAppUsage() { return appUsage; }

    public void setPeriodType(PeriodType type) {
        periodType.setValue(type);
        updatePeriodLabel();
        refreshStats();
    }

    public void setSelectedDate(long date) {
        selectedDate.setValue(DateUtils.getStartOfDay(date));
        updatePeriodLabel();
        refreshStats();
    }

    public void setBreakdownType(BreakdownType type) {
        breakdownType.setValue(type);
        refreshStats();
    }

    public void setSelectedPackageName(String packageName) {
        selectedPackageName.setValue(packageName);
    }

    public void navigatePeriod(int delta) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDate.getValue());
        switch (periodType.getValue()) {
            case DAY: cal.add(Calendar.DAY_OF_YEAR, delta); break;
            case WEEK: cal.add(Calendar.WEEK_OF_YEAR, delta); break;
            case MONTH: cal.add(Calendar.MONTH, delta); break;
        }
        setSelectedDate(cal.getTimeInMillis());
    }

    private long getRangeStart() {
        long date = selectedDate.getValue();
        switch (periodType.getValue()) {
            case WEEK: return DateUtils.getStartOfWeek(date);
            case MONTH: return DateUtils.getStartOfMonth(date);
            default: return date;
        }
    }

    private long getRangeEnd() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getRangeStart());
        switch (periodType.getValue()) {
            case DAY: cal.add(Calendar.DAY_OF_YEAR, 1); break;
            case WEEK: cal.add(Calendar.WEEK_OF_YEAR, 1); break;
            case MONTH: cal.add(Calendar.MONTH, 1); break;
        }
        return cal.getTimeInMillis();
    }

    private void updatePeriodLabel() {
        long date = selectedDate.getValue();
        PeriodType type = periodType.getValue();
        SimpleDateFormat sdf;
        if (type == PeriodType.DAY) {
            sdf = new SimpleDateFormat("dd MMM, yyyy", Locale.getDefault());
            periodLabel.setValue(sdf.format(date));
        } else if (type == PeriodType.WEEK) {
            long start = DateUtils.getStartOfWeek(date);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(start);
            cal.add(Calendar.DAY_OF_YEAR, 6);
            long end = cal.getTimeInMillis();
            sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            periodLabel.setValue(sdf.format(start) + " - " + sdf.format(end));
        } else {
            sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            periodLabel.setValue(sdf.format(date));
        }
    }

    public void refreshStats() {
        // Here we would calculate comparison with previous period
        // For now, providing basic stats
        // In a real implementation, we'd query the DB for the current and previous range
        // Since Room returns LiveData, we might need a more complex mediator or just a background query
        calculateOverviewAsync();
    }

    private void calculateOverviewAsync() {
        long start = getRangeStart();
        long end = getRangeEnd();
        PeriodType type = periodType.getValue();
        BreakdownType bType = breakdownType.getValue();
        
        // Previous range
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start);
        if (type == PeriodType.DAY) cal.add(Calendar.DAY_OF_YEAR, -1);
        else if (type == PeriodType.WEEK) cal.add(Calendar.WEEK_OF_YEAR, -1);
        else if (type == PeriodType.MONTH) cal.add(Calendar.MONTH, -1);
        
        long prevStart = cal.getTimeInMillis();
        if (type == PeriodType.WEEK) prevStart = DateUtils.getStartOfWeek(prevStart);
        if (type == PeriodType.MONTH) prevStart = DateUtils.getStartOfMonth(prevStart);
        long prevStartFinal = prevStart;
        long prevEndFinal = start;

        new Thread(() -> {
            ScreenTimeDao dao = AppDatabase.getInstance(getApplication()).screenTimeDao();
            List<ScreenTimeDao.HourlyUsageSummary> currentData = dao.getHourlyUsageSummarySync(start, end, null);
            List<ScreenTimeDao.HourlyUsageSummary> prevData = dao.getHourlyUsageSummarySync(prevStartFinal, prevEndFinal, null);

            double currentTotal = 0;
            for (ScreenTimeDao.HourlyUsageSummary h : currentData) {
                if (bType == BreakdownType.SCREEN_TIME) currentTotal += h.totalDuration;
                else if (bType == BreakdownType.TIMES_OPENED) currentTotal += h.totalOpens;
                else currentTotal += h.totalNotifications;
            }

            double prevTotal = 0;
            for (ScreenTimeDao.HourlyUsageSummary h : prevData) {
                if (bType == BreakdownType.SCREEN_TIME) prevTotal += h.totalDuration;
                else if (bType == BreakdownType.TIMES_OPENED) prevTotal += h.totalOpens;
                else prevTotal += h.totalNotifications;
            }

            ScreenTimeStats s = new ScreenTimeStats();
            
            // Days in current range
            int days = 1;
            if (type == PeriodType.WEEK) days = 7;
            else if (type == PeriodType.MONTH) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(start);
                days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            }
            
            s.avgValue = currentTotal / days;
            s.percentChange = prevTotal > 0 ? ((currentTotal - prevTotal) / prevTotal) * 100 : 0;

            // Target Met/Not Met (Only for Screen Time and for Week/Month)
            if (bType == BreakdownType.SCREEN_TIME && type != PeriodType.DAY) {
                long target = 4 * 3600 * 1000L; // 4 hours
                Map<Long, Long> dayMap = new HashMap<>();
                for (ScreenTimeDao.HourlyUsageSummary h : currentData) {
                    long dayStart = DateUtils.getStartOfDay(h.hourStartTimestamp);
                    Long currentVal = dayMap.get(dayStart);
                    dayMap.put(dayStart, (currentVal != null ? currentVal : 0L) + h.totalDuration);
                }
                
                int met = 0, notMet = 0;
                long cur = start;
                while (cur < end && cur < System.currentTimeMillis()) {
                    Long dayTotalVal = dayMap.get(cur);
                    long dayTotal = dayTotalVal != null ? dayTotalVal : 0L;
                    if (dayTotal > 0) {
                        if (dayTotal <= target) met++;
                        else notMet++;
                    }
                    cur += 86400000L;
                }
                s.targetMetCount = met;
                s.targetNotMetCount = notMet;
            }

            stats.postValue(s);
        }).start();
    }

    public static class ScreenTimeStats {
        public double avgValue;
        public int targetMetCount;
        public int targetNotMetCount;
        public double percentChange;
    }
}
