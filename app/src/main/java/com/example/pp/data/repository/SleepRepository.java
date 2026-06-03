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
            SleepEntity sleepForThatDate = sleepDao.getSleepByDateSync(date);
            if (sleepForThatDate != null) {
                sleepForThatDate.startTime = start;
                sleepForThatDate.endTime = end;
                sleepForThatDate.targetWakeTime = target;
                sleepForThatDate.updatedAt = System.currentTimeMillis();
                sleepDao.update(sleepForThatDate);
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

    public SleepEntity getSleepByDateSync(long date) {
        return sleepDao.getSleepByDateSync(date);
    }

    public LiveData<List<SleepEntity>> getSleepHistory(long start, long end) {
        return sleepDao.getSleepInRange(start, end);
    }

    public List<SleepEntity> getSleepHistorySync(long start, long end) {
        return sleepDao.getSleepInRangeSync(start, end);
    }

    public void updateDelayReason(long now, String reason) {
        executors.diskIO().execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(now);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            
            long sleepDate;
            if (hour < 12) {
                // It's early morning, the delay reason is for the previous night's sleep entry
                sleepDate = DateUtils.getStartOfDay(now - 24 * 3600 * 1000L);
            } else {
                // It's evening, the delay reason is for tonight's sleep entry
                sleepDate = DateUtils.getStartOfDay(now);
            }
            
            SleepEntity entity = sleepDao.getSleepByDateSync(sleepDate);
            if (entity != null) {
                sleepDao.updateDelayReason(sleepDate, reason);
            } else {
                // Create a placeholder entry if it doesn't exist yet
                SleepEntity placeholder = new SleepEntity(0, 0, 0, sleepDate);
                placeholder.setDelayReason(reason);
                sleepDao.insert(placeholder);
            }
        });
    }

    public void updateDelayReasonByDate(long date, String reason) {
        executors.diskIO().execute(() -> {
            sleepDao.updateDelayReason(date, reason);
        });
    }

    public void seedSleep() {
        executors.diskIO().execute(() -> {
            long now = System.currentTimeMillis();
            long todayDate = DateUtils.getStartOfDay(now);

            // Check if entry already exists for todayDate to avoid duplicates
            if (sleepDao.getSleepByDateSync(todayDate) != null) {
                return;
            }

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 6);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long targetWakeTime = cal.getTimeInMillis();

            sleepDao.insert(new SleepEntity(0, 0, targetWakeTime, todayDate));
        });
    }

    // Determine Target Wake Time
    public long getLatestTargetWakeTime() {
        SleepEntity lastSleep = getLastSleepSync();
        return lastSleep.getTargetWakeTime();
    }
}
