package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.pp.data.local.entity.QuotesApiTrack;
import com.example.pp.data.local.entity.SavedQuote;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface QuotesDao {
    @Insert
    void insert(QuotesApiTrack quote);

    @Query("SELECT * FROM quotes_api_track ORDER BY fetch_timestamp DESC LIMIT 1")
    QuotesApiTrack getLatestQuote();

    @Query("SELECT COUNT(*) FROM quotes_api_track")
    int getCount();

    @Query("SELECT * FROM quotes_api_track")
    List<QuotesApiTrack> getAllQuotesList();

    @Query("SELECT * FROM saved_quotes")
    List<SavedQuote> getAllSavedQuotesList();

    // Saved Quotes Methods
    @Insert
    void insertSavedQuotesBatch(List<SavedQuote> quotes);

    @Query("SELECT DISTINCT category FROM saved_quotes")
    LiveData<List<String>> getSavedDistinctCategories();

    @Query("SELECT DISTINCT category FROM quotes_api_track WHERE category IS NOT NULL AND category != ''")
    LiveData<List<String>> getApiDistinctCategories();

    @Query("SELECT * FROM saved_quotes ORDER BY created_date DESC LIMIT :pageSize OFFSET :offset")
    LiveData<List<SavedQuote>> getSavedQuotesPaged(int pageSize, int offset);

    @Query("SELECT * FROM saved_quotes WHERE category = :category ORDER BY created_date DESC LIMIT :pageSize OFFSET :offset")
    LiveData<List<SavedQuote>> getSavedQuotesByCategoryPaged(String category, int pageSize, int offset);

    @Query("SELECT COUNT(*) FROM saved_quotes")
    LiveData<Integer> getSavedQuotesCount();

    @Query("SELECT COUNT(*) FROM saved_quotes")
    int getSavedQuotesCountSync();

    @Query("SELECT COUNT(*) FROM saved_quotes WHERE category = :category")
    LiveData<Integer> getSavedQuotesCountByCategory(String category);

    @Query("SELECT * FROM saved_quotes WHERE category IN (:categories) ORDER BY created_date DESC LIMIT :pageSize OFFSET :offset")
    LiveData<List<SavedQuote>> getSavedQuotesByMultipleCategoriesPaged(List<String> categories, int pageSize, int offset);

    @Query("SELECT COUNT(*) FROM saved_quotes WHERE category IN (:categories)")
    LiveData<Integer> getSavedQuotesCountByMultipleCategories(List<String> categories);

    @Query("SELECT EXISTS(SELECT 1 FROM saved_quotes WHERE quote = :quote)")
    boolean existsByQuote(String quote);

    @Query("DELETE FROM saved_quotes WHERE quote = :quote")
    void deleteByQuote(String quote);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertBatch(List<QuotesApiTrack> quotes);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertSavedBatch(List<SavedQuote> quotes);

    @Transaction
    default SyncStats upsertAll(List<QuotesApiTrack> quotes) {
        SyncStats stats = new SyncStats("Quotes Track");
        stats.previousCount = getCount();
        if (quotes == null || quotes.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        upsertBatch(quotes);

        stats.inserted = quotes.size();
        stats.finalCount = getCount();
        return stats;
    }

    @Transaction
    default SyncStats upsertAllSavedQuotes(List<SavedQuote> quotes) {
        SyncStats stats = new SyncStats("Saved Quotes");
        stats.previousCount = getSavedQuotesCountSync();
        if (quotes == null || quotes.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        upsertSavedBatch(quotes);

        stats.inserted = quotes.size();
        stats.finalCount = getSavedQuotesCountSync();
        return stats;
    }
}
