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

import java.util.List;

@Dao
public interface SleepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SleepEntity sleep);

    @Update
    void update(SleepEntity sleep);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBatch(List<SleepEntity> items);

    @Query("SELECT COUNT(*) FROM sleep")
    int getCount();

    @Transaction
    default SyncStats upsertAll(List<SleepEntity> items) {
        SyncStats stats = new SyncStats("Sleep");
        stats.previousCount = getCount();
        if (items != null && !items.isEmpty()) {
            insertBatch(items);
            stats.inserted = items.size();
        }
        stats.finalCount = getCount();
        return stats;
    }

    @Query("SELECT * FROM sleep ORDER BY id DESC LIMIT 1")
    SleepEntity getLastSleepSync();

    @Query("SELECT * FROM sleep ORDER BY id DESC LIMIT 1")
    LiveData<SleepEntity> getLastSleep();

    @Query("SELECT * FROM sleep WHERE start_time >= :start AND start_time <= :end ORDER BY start_time ASC")
    LiveData<List<SleepEntity>> getSleepInRange(long start, long end);

    @Query("SELECT * FROM sleep")
    List<SleepEntity> getAllSleepEntries();
}
