package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.pp.data.local.entity.ScreenTime;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface ScreenTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ScreenTime entry);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ScreenTime> entries);

    @Query("SELECT COUNT(*) FROM screen_time")
    int getCount();

    @Transaction
    default SyncStats upsertAll(List<ScreenTime> items) {
        SyncStats stats = new SyncStats("ScreenTime");
        stats.previousCount = getCount();
        if (items != null && !items.isEmpty()) {
            insertAll(items);
            stats.inserted = items.size();
        }
        stats.finalCount = getCount();
        return stats;
    }

    @Query("SELECT * FROM screen_time WHERE hourStartTimestamp >= :start AND hourStartTimestamp < :end")
    List<ScreenTime> getEntriesInRangeSync(long start, long end);

    @Query("SELECT * FROM screen_time WHERE hourStartTimestamp >= :start AND hourStartTimestamp < :end")
    LiveData<List<ScreenTime>> getEntriesInRange(long start, long end);

    @Query("SELECT packageName, SUM(durationMillis) as totalDuration, SUM(timesOpened) as totalOpens, SUM(notificationsReceived) as totalNotifications FROM screen_time " +
           "WHERE hourStartTimestamp >= :start AND hourStartTimestamp < :end " +
           "GROUP BY packageName ORDER BY totalDuration DESC")
    LiveData<List<AppUsageSummary>> getAppUsageSummary(long start, long end);

    @Query("SELECT hourStartTimestamp, SUM(durationMillis) as totalDuration, SUM(timesOpened) as totalOpens, SUM(notificationsReceived) as totalNotifications FROM screen_time " +
           "WHERE hourStartTimestamp >= :start AND hourStartTimestamp < :end " +
           "AND (:packageName IS NULL OR packageName = :packageName) " +
           "GROUP BY hourStartTimestamp ORDER BY hourStartTimestamp ASC")
    LiveData<List<HourlyUsageSummary>> getHourlyUsageSummary(long start, long end, String packageName);

    @Query("SELECT hourStartTimestamp, SUM(durationMillis) as totalDuration, SUM(timesOpened) as totalOpens, SUM(notificationsReceived) as totalNotifications FROM screen_time " +
           "WHERE hourStartTimestamp >= :start AND hourStartTimestamp < :end " +
           "AND (:packageName IS NULL OR packageName = :packageName) " +
           "GROUP BY hourStartTimestamp ORDER BY hourStartTimestamp ASC")
    List<HourlyUsageSummary> getHourlyUsageSummarySync(long start, long end, String packageName);

    @Query("DELETE FROM screen_time WHERE packageName = :packageName AND hourStartTimestamp = :hourStart")
    void deleteEntry(String packageName, long hourStart);

    @Query("SELECT * FROM screen_time")
    List<ScreenTime> getAllEntriesSync();

    static class AppUsageSummary {
        public String packageName;
        public long totalDuration;
        public int totalOpens;
        public int totalNotifications;
    }

    static class HourlyUsageSummary {
        public long hourStartTimestamp;
        public long totalDuration;
        public int totalOpens;
        public int totalNotifications;
    }
}
