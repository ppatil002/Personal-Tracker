package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.pp.data.local.entity.HistoryApiTrack;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert
    void insert(HistoryApiTrack history);

    @Query("SELECT * FROM history_api_track ORDER BY fetch_timestamp DESC")
    LiveData<List<HistoryApiTrack>> getAllHistory();

    @Query("SELECT * FROM history_api_track WHERE history_date = :date LIMIT 1")
    HistoryApiTrack getHistoryByDate(String date);

    @Query("SELECT COUNT(*) FROM history_api_track")
    int getCount();

    @Query("SELECT * FROM history_api_track")
    List<HistoryApiTrack> getAllHistoryList();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertBatch(List<HistoryApiTrack> history);

    @Transaction
    default SyncStats upsertAll(List<HistoryApiTrack> history) {
        SyncStats stats = new SyncStats("History Track");
        stats.previousCount = getCount();
        if (history == null || history.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        upsertBatch(history);

        stats.inserted = history.size();
        stats.finalCount = getCount();
        return stats;
    }
}
