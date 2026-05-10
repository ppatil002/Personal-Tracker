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

import java.util.List;

import androidx.sqlite.db.SupportSQLiteQuery;

@Dao
public interface TransactionDao {

    @RawQuery
    List<TransactionEntity> getTableData(SupportSQLiteQuery query);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TransactionEntity transaction);

    @Update
    void update(TransactionEntity transaction);

    @Delete
    void delete(TransactionEntity transaction);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTagCrossRef(TransactionTagCrossRef crossRef);

    @Query("DELETE FROM transaction_tag_cross_ref WHERE transaction_id = :transactionId")
    void deleteTagCrossRefsForTransaction(String transactionId);

    @Query("SELECT * FROM transactions ORDER BY transaction_date DESC, created_timestamp DESC")
    LiveData<List<TransactionEntity>> getAllTransactions();

    @Query("SELECT * FROM transactions WHERE transaction_id = :id")
    TransactionEntity getTransactionById(String id);

    @Query("SELECT * FROM transactions WHERE category_id = :categoryId")
    List<TransactionEntity> getTransactionsByCategory(String categoryId);

    @Query("SELECT * FROM transactions WHERE transaction_date = :date")
    List<TransactionEntity> getTransactionsByDate(String date);

    @Transaction
    default void insertTransactionWithTags(TransactionEntity transaction, List<String> tagIds) {
        insert(transaction);
        deleteTagCrossRefsForTransaction(transaction.getTransactionId());
        for (String tagId : tagIds) {
            insertTagCrossRef(new TransactionTagCrossRef(transaction.getTransactionId(), tagId));
        }
    }
}