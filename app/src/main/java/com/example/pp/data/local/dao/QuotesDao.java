package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.pp.data.local.entity.QuotesApiTrack;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface QuotesDao {
    @Insert
    void insert(QuotesApiTrack quote);

    @Query("SELECT * FROM quotes_api_track ORDER BY fetch_timestamp DESC")
    LiveData<List<QuotesApiTrack>> getAllQuotes();

    @Query("SELECT * FROM quotes_api_track ORDER BY fetch_timestamp DESC LIMIT 1")
    QuotesApiTrack getLatestQuote();

    @Query("SELECT COUNT(*) FROM quotes_api_track")
    int getCount();

    @Query("SELECT * FROM quotes_api_track")
    List<QuotesApiTrack> getAllQuotesList();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertBatch(List<QuotesApiTrack> quotes);

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
}
