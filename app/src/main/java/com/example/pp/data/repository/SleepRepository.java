package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.SleepDao;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.utils.DateUtils;

import java.util.Calendar;
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
            // The sleep date is always the night before wake up
            long sleepDate = DateUtils.getStartOfDay(targetWakeTime - 24 * 3600 * 1000L);
            if (current != null && current.endTime == 0) {
                current.startTime = startTime;
                current.targetWakeTime = targetWakeTime;
                current.date = sleepDate;
                sleepDao.update(current);
            } else {
                sleepDao.insert(new SleepEntity(startTime, 0, targetWakeTime, sleepDate));
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
            // The sleep date is always the night before wake up
            long sleepDate = DateUtils.getStartOfDay(targetWakeTime - 24 * 3600 * 1000L);
            if (current != null && current.endTime == 0) {
                current.targetWakeTime = targetWakeTime;
                current.date = sleepDate;
                sleepDao.update(current);
            } else {
                sleepDao.insert(new SleepEntity(0, 0, targetWakeTime, sleepDate));
            }
        });
    }

    public void saveManualSleepEntry(long start, long end, long target, long date) {
        executors.diskIO().execute(() -> {
            SleepEntity current = sleepDao.getLastSleepSync();
            if (current != null && current.date == date) {
                current.startTime = start;
                current.endTime = end;
                current.targetWakeTime = target;
                current.updatedAt = System.currentTimeMillis();
                sleepDao.update(current);
            } else {
                sleepDao.insert(new SleepEntity(start, end, target, date));
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

    // Determine Target Wake Time
    public long getTargetWakeTime(long now) {
        SleepEntity lastSleep = getLastSleepSync();
        long targetWakeTime;

        // User is sleeping but not wakeup
        if (lastSleep != null && lastSleep.endTime == 0) {
            targetWakeTime = lastSleep.targetWakeTime;
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 6);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= now) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            targetWakeTime = cal.getTimeInMillis();
        }
        return targetWakeTime;
    }
}
