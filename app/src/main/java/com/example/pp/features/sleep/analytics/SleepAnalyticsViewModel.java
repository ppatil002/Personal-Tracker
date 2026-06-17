package com.example.pp.features.sleep.analytics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.PPApplication;
import com.example.pp.core.enums.PeriodType;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.repository.SleepRepository;
import com.example.pp.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SleepAnalyticsViewModel extends AndroidViewModel {

    private final SleepRepository repository;
    private final MutableLiveData<PeriodType> periodType = new MutableLiveData<>(PeriodType.WEEK);
    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>(System.currentTimeMillis());
    private final MutableLiveData<String> periodLabel = new MutableLiveData<>("");
    private final MutableLiveData<SleepStats> stats = new MutableLiveData<>();
    private final MediatorLiveData<List<SleepEntity>> sleepData = new MediatorLiveData<>();

    private LiveData<List<SleepEntity>> currentDataSource;

    public SleepAnalyticsViewModel(@NonNull Application application) {
        super(application);
        repository = ((PPApplication) application).getSleepRepository();

        sleepData.addSource(periodType, t -> refreshData());
        sleepData.addSource(selectedDate, d -> refreshData());
        
        refreshData();
    }

    private void refreshData() {
        updatePeriodLabel();
        
        if (currentDataSource != null) {
            sleepData.removeSource(currentDataSource);
        }

        long start = getRangeStart();
        long end = getRangeEnd();
        
        currentDataSource = repository.getSleepHistory(start, end);
        sleepData.addSource(currentDataSource, data -> {
            sleepData.setValue(data);
            calculateStats(data);
        });
    }

    private void calculateStats(List<SleepEntity> currentData) {
        long start = getRangeStart();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start);
        PeriodType type = periodType.getValue();
        if (type == PeriodType.WEEK) cal.add(Calendar.WEEK_OF_YEAR, -1);
        else if (type == PeriodType.MONTH) cal.add(Calendar.MONTH, -1);

        final long prevStartFinal;
        if (type == PeriodType.WEEK) prevStartFinal = DateUtils.getStartOfWeek(cal.getTimeInMillis());
        else prevStartFinal = DateUtils.getStartOfMonth(cal.getTimeInMillis());

        final long prevEndFinal = start;

        new Thread(() -> {
            List<SleepEntity> prevData = repository.getSleepHistorySync(prevStartFinal, prevEndFinal);
            
            double currentTotal = 0;
            Set<Long> currentUniqueDays = new HashSet<>();
            if (currentData != null) {
                for (SleepEntity s : currentData) {
                    if (s.endTime > s.startTime) {
                        currentTotal += (s.endTime - s.startTime);
                        currentUniqueDays.add(DateUtils.getStartOfDay(s.date));
                    }
                }
            }

            double prevTotal = 0;
            Set<Long> prevUniqueDays = new HashSet<>();
            if (prevData != null) {
                for (SleepEntity s : prevData) {
                    if (s.endTime > s.startTime) {
                        prevTotal += (s.endTime - s.startTime);
                        prevUniqueDays.add(DateUtils.getStartOfDay(s.date));
                    }
                }
            }

            int currentDaysCount = currentUniqueDays.size();
            int prevDaysCount = prevUniqueDays.size();

            double currentAvg = currentDaysCount > 0 ? currentTotal / currentDaysCount : 0;
            double prevAvg = prevDaysCount > 0 ? prevTotal / prevDaysCount : 0;

            SleepStats s = new SleepStats();
            s.avgValue = currentAvg;
            // Percent change should be based on average sleep duration, not total,
            // otherwise comparing a partial week to a full week would be misleading.
            s.percentChange = prevAvg > 0 ? ((currentAvg - prevAvg) / prevAvg) * 100 : 0;
            stats.postValue(s);
        }).start();
    }

    private void updatePeriodLabel() {
        Long dateVal = selectedDate.getValue();
        long date = dateVal != null ? dateVal : System.currentTimeMillis();
        PeriodType type = periodType.getValue();
        
        if (type == PeriodType.WEEK) {
            long start = DateUtils.getStartOfWeek(date);
            long end = start + 6 * 24 * 3600 * 1000L;
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            periodLabel.setValue(sdf.format(start) + " - " + sdf.format(end));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            periodLabel.setValue(sdf.format(date));
        }
    }

    public void navigatePeriod(int delta) {
        Long current = selectedDate.getValue();
        if (current == null) current = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(current);
        if (periodType.getValue() == PeriodType.WEEK) {
            cal.add(Calendar.WEEK_OF_YEAR, delta);
        } else {
            cal.add(Calendar.MONTH, delta);
        }
        selectedDate.setValue(cal.getTimeInMillis());
    }

    public long getRangeStart() {
        Long dateVal = selectedDate.getValue();
        long date = dateVal != null ? dateVal : System.currentTimeMillis();
        if (periodType.getValue() == PeriodType.WEEK) return DateUtils.getStartOfWeek(date);
        return DateUtils.getStartOfMonth(date);
    }

    public long getRangeEnd() {
        long start = getRangeStart();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start);
        if (periodType.getValue() == PeriodType.WEEK) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        } else {
            cal.add(Calendar.MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    public LiveData<PeriodType> getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType type) { periodType.setValue(type); }
    public LiveData<Long> getSelectedDate() { return selectedDate; }
    public void setSelectedDate(long date) { selectedDate.setValue(date); }
    public LiveData<String> getPeriodLabel() { return periodLabel; }
    public LiveData<SleepStats> getStats() { return stats; }
    public LiveData<List<SleepEntity>> getSleepData() { return sleepData; }

    public void updateDelayReason(long date, String reason) {
        repository.updateDelayReasonByDate(date, reason);
    }

    public static class SleepStats {
        public double avgValue; // in millis
        public double percentChange;
    }
}
