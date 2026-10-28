package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.HydrationDao;
import com.example.pp.data.local.entity.HydrationIntake;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HydrationRepository {
    private final HydrationDao hydrationDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HydrationRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.hydrationDao = db.hydrationDao();
    }

    private long adjustTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 3) {
            // Register as previous day 11:59 PM
            cal.add(Calendar.DAY_OF_YEAR, -1);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }
        return timestamp;
    }

    public void addIntakeSync(int amountMl, long timestamp) {
        long adjusted = adjustTimestamp(timestamp);
        hydrationDao.insert(new HydrationIntake(amountMl, adjusted));
    }

    public void addIntake(int amountMl, long timestamp) {
        long adjusted = adjustTimestamp(timestamp);
        executor.execute(() -> {
            hydrationDao.insert(new HydrationIntake(amountMl, adjusted));
        });
    }

    public HydrationIntake findLatestIntake() {
        return hydrationDao.findLatestIntake();
    }

    public LiveData<Integer> getTodayTotalML(long startOfDay) {
        return hydrationDao.getTotalMLSince(startOfDay);
    }

    public int getTodayTotalMLSync(long startOfDay) {
        Integer total = hydrationDao.getTotalMLSinceSync(startOfDay);
        return total != null ? total : 0;
    }

    public LiveData<Integer> getIntakeCountSince(long startOfDay) {
        return hydrationDao.getIntakeCountSince(startOfDay);
    }

    public LiveData<HydrationIntake> getLastIntakeForDate(long startOfDay) {
        return hydrationDao.getLatestIntakeForDate(startOfDay);
    }

    public LiveData<List<HydrationIntake>> getIntakesInRange(long start, long end) {
        return hydrationDao.getIntakesInRange(start, end);
    }

    public LiveData<List<HydrationIntake>> getGroupedIntakesInRange(long start, long end) {
        return hydrationDao.getGroupedIntakesInRange(start, end);
    }

    public List<HydrationIntake> getIntakesInRangeSync(long start, long end) {
        return hydrationDao.getIntakesInRangeSync(start, end);
    }

    public void updateIntake(HydrationIntake intake) {
        executor.execute(() -> hydrationDao.update(intake));
    }

    public void deleteIntake(HydrationIntake intake) {
        executor.execute(() -> hydrationDao.delete(intake));
    }

    public void updateIntakeTimestamp(long start, long end, long newTimestamp) {
        executor.execute(() -> hydrationDao.updateIntakeTimestamp(start, end, newTimestamp));
    }
}
