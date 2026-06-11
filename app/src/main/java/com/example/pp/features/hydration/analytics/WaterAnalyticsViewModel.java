package com.example.pp.features.hydration.analytics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pp.PPApplication;
import com.example.pp.core.enums.PeriodType;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.repository.HydrationRepository;
import com.example.pp.utils.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class WaterAnalyticsViewModel extends AndroidViewModel {
    private final HydrationRepository repository;
    private final MutableLiveData<PeriodType> periodType = new MutableLiveData<>(PeriodType.WEEK);
    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>(DateUtils.getStartOfDay(System.currentTimeMillis()));
    private final MutableLiveData<String> periodLabel = new MutableLiveData<>("");
    private final MediatorLiveData<List<HydrationIntake>> hydrationData = new MediatorLiveData<>();
    private LiveData<List<HydrationIntake>> currentDataSource;

    public WaterAnalyticsViewModel(@NonNull Application application) {
        super(application);
        repository = ((PPApplication) application).getHydrationRepository();
        
        hydrationData.addSource(periodType, type -> refreshData());
        hydrationData.addSource(selectedDate, date -> refreshData());
    }

    private void refreshData() {
        if (currentDataSource != null) {
            hydrationData.removeSource(currentDataSource);
        }
        
        long start = getRangeStart();
        long end = getRangeEnd();
        
        currentDataSource = repository.getIntakesInRange(start, end);
        hydrationData.addSource(currentDataSource, hydrationData::setValue);
        updatePeriodLabel();
    }

    private void updatePeriodLabel() {
        PeriodType type = periodType.getValue();
        Long date = selectedDate.getValue();
        if (type == null || date == null) return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);

        if (type == PeriodType.WEEK) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            String startStr = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, 6);
            String endStr = sdf.format(cal.getTime());
            periodLabel.setValue(startStr + " - " + endStr);
        } else if (type == PeriodType.MONTH) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            periodLabel.setValue(sdf.format(cal.getTime()));
        }
    }

    public void navigatePeriod(int delta) {
        Long current = selectedDate.getValue();
        if (current == null) return;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(current);
        
        PeriodType type = periodType.getValue();
        if (type == PeriodType.WEEK) {
            cal.add(Calendar.WEEK_OF_YEAR, delta);
        } else if (type == PeriodType.MONTH) {
            cal.add(Calendar.MONTH, delta);
        }
        selectedDate.setValue(cal.getTimeInMillis());
    }

    public long getRangeStart() {
        PeriodType type = periodType.getValue();
        Long date = selectedDate.getValue();
        if (type == null || date == null) return 0;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (type == PeriodType.WEEK) {
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        } else if (type == PeriodType.MONTH) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    public long getRangeEnd() {
        long start = getRangeStart();
        PeriodType type = periodType.getValue();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(start);
        
        if (type == PeriodType.WEEK) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        } else if (type == PeriodType.MONTH) {
            cal.add(Calendar.MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    public LiveData<PeriodType> getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType type) { periodType.setValue(type); }
    public LiveData<Long> getSelectedDate() { return selectedDate; }
    public void setSelectedDate(long date) { selectedDate.setValue(date); }
    public LiveData<String> getPeriodLabel() { return periodLabel; }
    public LiveData<List<HydrationIntake>> getHydrationData() { return hydrationData; }
}
