package com.example.pp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.pp.data.local.entity.VocabularyRevisionLog;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface VocabularyRevisionLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(VocabularyRevisionLog log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBatch(List<VocabularyRevisionLog> logs);

    @Query("SELECT * FROM vocabulary_revision_logs WHERE vocabulary_id = :vocabularyId ORDER BY timestamp DESC")
    List<VocabularyRevisionLog> getLogsForVocabulary(long vocabularyId);

    @Query("SELECT * FROM vocabulary_revision_logs ORDER BY timestamp DESC")
    List<VocabularyRevisionLog> getAllLogs();

    @Query("SELECT COUNT(*) FROM vocabulary_revision_logs")
    int getCount();

    @Transaction
    default SyncStats upsertAll(List<VocabularyRevisionLog> items) {
        SyncStats stats = new SyncStats("VocabularyRevisionLog");
        stats.previousCount = getCount();
        if (items != null && !items.isEmpty()) {
            insertBatch(items);
            stats.inserted = items.size();
        }
        stats.finalCount = getCount();
        return stats;
    }
}
