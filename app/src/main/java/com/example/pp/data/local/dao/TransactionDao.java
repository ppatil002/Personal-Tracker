package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.pp.data.local.entity.TransactionEntity;
import com.example.pp.data.local.entity.TransactionTagCrossRef;
import com.example.pp.data.local.model.SyncStats;
import com.example.pp.features.analytics.model.MonthlyExpense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dao
public interface TransactionDao {

    @RawQuery
    List<TransactionEntity> getTableData(SupportSQLiteQuery query);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(TransactionEntity transaction);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertBatch(List<TransactionEntity> transactions);

    @Update
    void update(TransactionEntity transaction);

    @Update
    void updateBatch(List<TransactionEntity> transactions);

    @Delete
    void delete(TransactionEntity transaction);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTagCrossRef(TransactionTagCrossRef crossRef);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertBatchTagCrossRefs(List<TransactionTagCrossRef> crossRefs);

    @Update
    void updateTagCrossRef(TransactionTagCrossRef crossRef);

    @Update
    void updateBatchTagCrossRefs(List<TransactionTagCrossRef> crossRefs);

    @Query("DELETE FROM transaction_tag_cross_ref WHERE transaction_id = :transactionId")
    void deleteTagCrossRefsForTransaction(String transactionId);

    @Transaction
    default void deleteTransactionWithTags(TransactionEntity transaction) {
        deleteTagCrossRefsForTransaction(transaction.getTransactionId());
        delete(transaction);
    }

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC, created_timestamp DESC")
    LiveData<List<TransactionEntity>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE transaction_id = :id")
    TransactionEntity getTransactionById(String id);

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId")
    List<TransactionEntity> getTransactionsByCategory(String categoryId);

    @Query("SELECT * FROM transactions WHERE transaction_date = :date")
    List<TransactionEntity> getTransactionsByDate(String date);

    @Query("SELECT * FROM transactions")
    List<TransactionEntity> getAllTransactionsList();

    @Query("SELECT * FROM transactions WHERE transaction_date >= :start AND transaction_date < :end ORDER BY transaction_date ASC")
    List<TransactionEntity> getTransactionsBetween(long start, long end);

    @Query("SELECT CAST(strftime('%m', transaction_date / 1000, 'unixepoch', 'localtime') AS INTEGER) as month, " +
           "SUM(amount) as totalAmount " +
           "FROM transactions " +
           "WHERE transaction_date >= :start AND transaction_date < :end " +
           "GROUP BY month " +
           "ORDER BY month ASC")
    List<MonthlyExpense> getMonthlyExpensesBetween(long start, long end);

    @Query("SELECT COUNT(*) FROM transactions WHERE transaction_date >= :start AND transaction_date < :end")
    int getTransactionCountBetween(long start, long end);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE transaction_date >= :start AND transaction_date < :end")
    double getTotalAmountBetween(long start, long end);

    @Query("SELECT * FROM transaction_tag_cross_ref WHERE transaction_id IN (SELECT transaction_id FROM transactions WHERE transaction_date >= :start AND transaction_date < :end)")
    List<TransactionTagCrossRef> getTagCrossRefsBetween(long start, long end);

    @Query("SELECT * FROM transaction_tag_cross_ref")
    List<TransactionTagCrossRef> getAllTransactionTagCrossRefs();

    @Query("SELECT * FROM transaction_tag_cross_ref WHERE transaction_id = :txId AND tag_id = :tagId")
    TransactionTagCrossRef getTagCrossRefByIds(String txId, String tagId);

    @Query("SELECT COUNT(*) FROM transactions")
    int getTransactionCount();

    @Query("SELECT COUNT(*) FROM transaction_tag_cross_ref")
    int getTagCrossRefCount();

    @Transaction
    default SyncStats upsertAll(List<TransactionEntity> cloudItems) {
        SyncStats stats = new SyncStats("Transactions");
        stats.previousCount = getTransactionCount();
        if (cloudItems == null || cloudItems.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        List<TransactionEntity> localItems = getAllTransactionsList();
        Map<String, TransactionEntity> localMap = new HashMap<>();
        for (TransactionEntity local : localItems) {
            localMap.put(local.getTransactionId(), local);
        }

        List<TransactionEntity> toInsert = new ArrayList<>();
        List<TransactionEntity> toUpdate = new ArrayList<>();

        for (TransactionEntity cloud : cloudItems) {
            TransactionEntity local = localMap.get(cloud.getTransactionId());
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
        stats.finalCount = getTransactionCount();
        return stats;
    }

    @Transaction
    default SyncStats upsertAllTagCrossRefs(List<TransactionTagCrossRef> cloudItems) {
        SyncStats stats = new SyncStats("Transaction Tags");
        stats.previousCount = getTagCrossRefCount();
        if (cloudItems == null || cloudItems.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        List<TransactionTagCrossRef> localItems = getAllTransactionTagCrossRefs();
        Map<String, TransactionTagCrossRef> localMap = new HashMap<>();
        for (TransactionTagCrossRef local : localItems) {
            localMap.put(local.getTransactionId() + "|" + local.getTagId(), local);
        }

        List<TransactionTagCrossRef> toInsert = new ArrayList<>();
        List<TransactionTagCrossRef> toUpdate = new ArrayList<>();

        for (TransactionTagCrossRef cloud : cloudItems) {
            TransactionTagCrossRef local = localMap.get(cloud.getTransactionId() + "|" + cloud.getTagId());
            if (local == null) {
                toInsert.add(cloud);
            } else if (cloud.getLastUpdatedTimestamp() > local.getLastUpdatedTimestamp()) {
                toUpdate.add(cloud);
            }
        }

        if (!toInsert.isEmpty()) insertBatchTagCrossRefs(toInsert);
        if (!toUpdate.isEmpty()) updateBatchTagCrossRefs(toUpdate);

        stats.inserted = toInsert.size();
        stats.updated = toUpdate.size();
        stats.finalCount = getTagCrossRefCount();
        return stats;
    }

    @Transaction
    default void insertTransactionWithTags(TransactionEntity transaction, List<String> tagIds) {
        insert(transaction);
        deleteTagCrossRefsForTransaction(transaction.getTransactionId());
        long now = System.currentTimeMillis();
        for (String tagId : tagIds) {
            insertTagCrossRef(new TransactionTagCrossRef(transaction.getTransactionId(), tagId, now));
        }
    }
}
