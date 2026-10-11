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

    @Query("SELECT * FROM vocabulary WHERE id = :id LIMIT 1")
    Vocabulary getVocabById(long id);

    @Query("SELECT * FROM vocabulary WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    Vocabulary getVocabByWord(String word);

    @Query("SELECT * FROM vocabulary ORDER BY created_timestamp DESC LIMIT 1")
    Vocabulary getLastAddedVocab();

    @Query("SELECT * FROM vocabulary WHERE next_revision_timestamp <= :currentTimestamp ORDER BY next_revision_timestamp ASC LIMIT :limit")
    List<Vocabulary> getOverdueVocabulary(long currentTimestamp, int limit);

    @Query("SELECT * FROM vocabulary ORDER BY last_reviewed_timestamp ASC LIMIT :limit")
    List<Vocabulary> getOldestReviewedVocabulary(int limit);

    @Query("SELECT * FROM vocabulary WHERE (meaning IS NULL OR TRIM(meaning) = '') OR (example IS NULL OR TRIM(example) = '') OR (part_of_speach IS NULL OR TRIM(part_of_speach) = '') ORDER BY created_timestamp DESC LIMIT :limit")
    List<Vocabulary> getIncompleteVocabularies(int limit);

    @Query("SELECT COUNT(*) FROM vocabulary")
    int getCount();

    @Query("UPDATE vocabulary SET last_reviewed_timestamp = 0, correct_attempts = 0, incorrect_attempts = 0, hints_used_count = 0, half_life = 0.0, next_revision_timestamp = 0")
    void resetAllVocabularyStats();

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
