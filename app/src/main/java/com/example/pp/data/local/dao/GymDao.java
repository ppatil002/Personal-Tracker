package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.pp.data.local.entity.BodyWeight;
import com.example.pp.data.local.entity.GymTracking;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface GymDao {

    // Body Weight
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBodyWeight(BodyWeight weight);

    @Update
    void updateBodyWeight(BodyWeight weight);

    @Query("SELECT * FROM body_weight WHERE date_measured = :date LIMIT 1")
    BodyWeight getWeightByDateSync(long date);

    @Query("SELECT * FROM body_weight ORDER BY date_measured DESC LIMIT :limit")
    LiveData<List<BodyWeight>> getLastWeights(int limit);

    @Query("SELECT * FROM body_weight ORDER BY date_measured DESC")
    LiveData<List<BodyWeight>> getAllWeights();

    // Gym Tracking
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGymLog(GymTracking log);

    @Delete
    void deleteGymLog(GymTracking log);

    @Query("SELECT * FROM gym_tracking ORDER BY exercise_date DESC")
    LiveData<List<GymTracking>> getAllGymLogs();

    @Query("SELECT * FROM gym_tracking WHERE exercise_date >= :startDate ORDER BY exercise_date DESC")
    LiveData<List<GymTracking>> getGymLogsFromDate(long startDate);

    @Query("SELECT * FROM gym_tracking WHERE exercise_date >= :start AND exercise_date < :end ORDER BY exercise_date ASC")
    LiveData<List<GymTracking>> getGymLogsForRange(long start, long end);

    @Query("SELECT DISTINCT CAST(exercise_date / 86400000 AS INT) * 86400000 FROM gym_tracking ORDER BY exercise_date DESC")
    LiveData<List<Long>> getUniqueWorkoutDates();

    @Query("SELECT COUNT(*) FROM body_weight")
    int getBodyWeightCount();

    @Query("SELECT COUNT(*) FROM gym_tracking")
    int getGymTrackingCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBodyWeightBatch(List<BodyWeight> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGymTrackingBatch(List<GymTracking> items);

    @Query("SELECT * FROM body_weight")
    List<BodyWeight> getAllWeightsSync();

    @Query("SELECT * FROM gym_tracking")
    List<GymTracking> getAllGymLogsSync();

    @Transaction
    default SyncStats upsertAllWeights(List<BodyWeight> items) {
        SyncStats stats = new SyncStats("Body Weight");
        stats.previousCount = getBodyWeightCount();
        if (items != null && !items.isEmpty()) {
            insertBodyWeightBatch(items);
            stats.inserted = items.size();
        }
        stats.finalCount = getBodyWeightCount();
        return stats;
    }

    @Transaction
    default SyncStats upsertAllGymLogs(List<GymTracking> items) {
        SyncStats stats = new SyncStats("Gym Tracking");
        stats.previousCount = getGymTrackingCount();
        if (items != null && !items.isEmpty()) {
            insertGymTrackingBatch(items);
            stats.inserted = items.size();
        }
        stats.finalCount = getGymTrackingCount();
        return stats;
    }
}
