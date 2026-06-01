package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.SleepDao;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.utils.DateUtils;

import java.util.List;

public class SleepRepository {
    private final SleepDao sleepDao;
    private final AppExecutors executors;

    public SleepRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.sleepDao = db.sleepDao();
        this.executors = AppExecutors.getInstance();
    }

    public void startSleep(long startTime, long targetWakeTime) {
        executors.diskIO().execute(() -> {
            SleepEntity current = sleepDao.getLastSleepSync();
            long date = DateUtils.getStartOfDay(startTime);
            if (current != null && current.endTime == 0) {
                current.startTime = startTime;
                current.targetWakeTime = targetWakeTime;
                current.date = date;
                sleepDao.update(current);
            } else {
                sleepDao.insert(new SleepEntity(startTime, 0, targetWakeTime, date));
            }
        });
    }

    public void stopSleep(long endTime) {
        executors.diskIO().execute(() -> {
            SleepEntity current = sleepDao.getLastSleepSync();
            if (current != null && current.endTime == 0) {
                current.endTime = endTime;
                sleepDao.update(current);
            }
        });
    }

    public void updateTargetWakeTime(long targetWakeTime) {
        executors.diskIO().execute(() -> {
            SleepEntity current = sleepDao.getLastSleepSync();
            long date = DateUtils.getStartOfDay(targetWakeTime); // Use target wake time for date reference
            if (current != null && current.endTime == 0) {
                current.targetWakeTime = targetWakeTime;
                current.date = date;
                sleepDao.update(current);
            } else {
                sleepDao.insert(new SleepEntity(0, 0, targetWakeTime, date));
            }
        });
    }

    public LiveData<SleepEntity> getLastSleep() {
        return sleepDao.getLastSleep();
    }

    public SleepEntity getLastSleepSync() {
        return sleepDao.getLastSleepSync();
    }

    public LiveData<List<SleepEntity>> getSleepHistory(long start, long end) {
        return sleepDao.getSleepInRange(start, end);
    }

    public long getTargetWakeTime(SleepEntity lastSleep, long now) {
        long targetWakeTime;

        // Determine Target Wake Time
        if (lastSleep != null && lastSleep.endTime == 0) {
            targetWakeTime = lastSleep.targetWakeTime;
        } else {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 6);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= now) {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
            }
            targetWakeTime = cal.getTimeInMillis();
        }
        return targetWakeTime;
    }
}
