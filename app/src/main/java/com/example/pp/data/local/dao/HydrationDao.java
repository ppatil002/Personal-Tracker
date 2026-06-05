package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface HydrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HydrationIntake intake);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBatch(List<HydrationIntake> intakes);

    @Query("SELECT COUNT(*) FROM hydration_intakes")
    int getCount();

    @Transaction
    default SyncStats upsertAll(List<HydrationIntake> items) {
        SyncStats stats = new SyncStats("Hydration");
        stats.previousCount = getCount();
        if (items != null && !items.isEmpty()) {
            insertBatch(items);
            stats.inserted = items.size();
        }
        stats.finalCount = getCount();
        return stats;
    }

    @Query("SELECT * FROM hydration_intakes ORDER BY timestamp DESC LIMIT 1")
    HydrationIntake findLatestIntake();

    @Query("SELECT SUM(amount_ml) FROM hydration_intakes WHERE timestamp >= :timestamp")
    LiveData<Integer> getTotalMLSince(long timestamp);

    @Query("SELECT COUNT(*) FROM hydration_intakes WHERE timestamp >= :timestamp")
    LiveData<Integer> getIntakeCountSince(long timestamp);

    @Query("SELECT * FROM hydration_intakes WHERE timestamp >= :timestamp ORDER BY timestamp DESC LIMIT 1")
    LiveData<HydrationIntake> getLatestIntakeForDate(long timestamp);

    @Query("SELECT * FROM hydration_intakes")
    List<HydrationIntake> getAllIntakes();
}
