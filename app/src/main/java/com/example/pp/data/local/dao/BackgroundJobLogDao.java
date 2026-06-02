package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.model.SyncStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dao
public interface BackgroundJobLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BackgroundJobLog log);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertBatch(List<BackgroundJobLog> logs);

    @Update
    void updateBatch(List<BackgroundJobLog> logs);

    @Query("SELECT * FROM background_job_log ORDER BY last_updated_timestamp DESC")
    LiveData<List<BackgroundJobLog>> getAllLogs();

    @Query("SELECT * FROM background_job_log")
    List<BackgroundJobLog> getAllLogsList();

    @Query("SELECT * FROM background_job_log WHERE job_id = :jobId")
    BackgroundJobLog getLogById(String jobId);

    @Query("SELECT COUNT(*) FROM background_job_log")
    int getCount();

    @Query("DELETE FROM background_job_log")
    void deleteAll();

    @Transaction
    default SyncStats upsertAll(List<BackgroundJobLog> cloudItems) {
        SyncStats stats = new SyncStats("Background Job Logs");
        stats.previousCount = getCount();
        if (cloudItems == null || cloudItems.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        List<BackgroundJobLog> localItems = getAllLogsList();
        Map<String, BackgroundJobLog> localMap = new HashMap<>();
        for (BackgroundJobLog local : localItems) {
            localMap.put(local.getJobId(), local);
        }

        List<BackgroundJobLog> toInsert = new ArrayList<>();
        List<BackgroundJobLog> toUpdate = new ArrayList<>();

        for (BackgroundJobLog cloud : cloudItems) {
            BackgroundJobLog local = localMap.get(cloud.getJobId());
            if (local == null) {
                toInsert.add(cloud);
            } else if (cloud.getLastUpdatedTimestamp() > local.getLastUpdatedTimestamp()) {
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

    @Query(
            "UPDATE background_job_log " +
                    "SET status = :status, " +
                    "    log_message = :logMessage, " +
                    "    last_updated_timestamp = :newTimestamp " +
                    "WHERE worker_name = :workerName " +
                    "AND current_step = :currentStep " +
                    "AND date(last_updated_timestamp / 1000, 'unixepoch', 'localtime') = " +
                    "    date(:newTimestamp / 1000, 'unixepoch', 'localtime')"
    )
    int updateExistingDailyLog(
            String workerName,
            String currentStep,
            String status,
            String logMessage,
            long newTimestamp
    );

}