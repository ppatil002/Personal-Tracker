package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.local.model.SyncStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dao
public interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SleepEntity sleep);

    @Update
    void update(SleepEntity sleep);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertBatch(List<SleepEntity> items);

    @Update
    void updateBatch(List<SleepEntity> items);

    @Query("SELECT COUNT(*) FROM sleep")
    int getCount();

    @Transaction
    default SyncStats upsertAll(List<SleepEntity> cloudItems) {
        SyncStats stats = new SyncStats("Sleep");
        stats.previousCount = getCount();
        if (cloudItems == null || cloudItems.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        List<SleepEntity> localItems = getAllSleepEntries();
        Map<Long, SleepEntity> localMap = new HashMap<>();
        for (SleepEntity local : localItems) {
            localMap.put(local.getDate(), local);
        }

        List<SleepEntity> toInsert = new ArrayList<>();
        List<SleepEntity> toUpdate = new ArrayList<>();

        for (SleepEntity cloud : cloudItems) {
            SleepEntity local = localMap.get(cloud.getDate());
            if (local == null) {
                toInsert.add(cloud);
            } else if (cloud.getUpdatedAt() > local.getUpdatedAt()) {
                toUpdate.add(cloud);
            }
        }

        if (!toInsert.isEmpty()) insertBatch(toInsert);
        if (!toUpdate.isEmpty()) updateBatch(toUpdate);

        stats.inserted = toInsert.size();
        stats.updated = toUpdate.size();
        stats.finalCount = getCount();
        return stats;
    }

    @Query("SELECT * FROM sleep ORDER BY date DESC LIMIT 1")
    SleepEntity getLastSleepSync();

    @Query("SELECT * FROM sleep ORDER BY date DESC LIMIT 1")
    LiveData<SleepEntity> getLastSleep();

    @Query("SELECT * FROM sleep WHERE start_time >= :start AND start_time <= :end ORDER BY start_time ASC")
    LiveData<List<SleepEntity>> getSleepInRange(long start, long end);

    @Query("SELECT * FROM sleep WHERE start_time >= :start AND start_time <= :end ORDER BY start_time ASC")
    List<SleepEntity> getSleepInRangeSync(long start, long end);

    @Query("SELECT * FROM sleep WHERE date = :date LIMIT 1")
    SleepEntity getSleepByDateSync(long date);

    @Query("UPDATE sleep SET delay_reason = :reason WHERE date = :date")
    void updateDelayReason(long date, String reason);

    @Query("SELECT * FROM sleep")
    List<SleepEntity> getAllSleepEntries();
}
