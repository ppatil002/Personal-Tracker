package com.example.pp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface VocabularyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Vocabulary vocabulary);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBatch(List<Vocabulary> vocabularies);

    @Delete
    void delete(Vocabulary vocabulary);

    @Query("SELECT * FROM vocabulary ORDER BY created_timestamp DESC LIMIT :limit OFFSET :offset")
    List<Vocabulary> getLatestVocab(int limit, int offset);

    @Query("SELECT * FROM vocabulary ORDER BY created_timestamp DESC")
    List<Vocabulary> getAllVocabList();

    @Query("SELECT * FROM vocabulary WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    Vocabulary getVocabByWord(String word);

    @Query("SELECT * FROM vocabulary ORDER BY created_timestamp DESC LIMIT 1")
    Vocabulary getLastAddedVocab();

    @Query("SELECT COUNT(*) FROM vocabulary")
    int getCount();

    @Transaction
    default SyncStats upsertAll(List<Vocabulary> items) {
        SyncStats stats = new SyncStats("Vocabulary");
        stats.previousCount = getCount();
        if (items != null && !items.isEmpty()) {
            insertBatch(items);
            stats.inserted = items.size();
        }
        stats.finalCount = getCount();
        return stats;
    }
}
