package com.example.pp.data.repository;

import android.content.Context;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.TransactionDao;
import com.example.pp.data.local.entity.TransactionEntity;
import com.example.pp.data.local.query.TableQueryBuilder;
import com.example.pp.ui.components.table.model.TableQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

public class TransactionRepository {
    private final TransactionDao transactionDao;
    private final AppExecutors executors;

    public TransactionRepository(Context context) {
        this.transactionDao = AppDatabase.getInstance(context).transactionDao();
        this.executors = AppExecutors.getInstance();
    }

    public List<TransactionEntity> getTableData(TableQuery query) {
        SupportSQLiteQuery sqliteQuery = TableQueryBuilder.build("transactions", query);
        return transactionDao.getTableData(sqliteQuery);
    }

    public void saveTransaction(TransactionEntity transaction, List<String> tagIds, Runnable onSuccess, Runnable onError) {
        executors.diskIO().execute(() -> {
            try {
                if (tagIds == null || tagIds.isEmpty()) {
                    transactionDao.insert(transaction);
                } else {
                    transactionDao.insertTransactionWithTags(transaction, tagIds);
                }
                executors.mainThread().execute(onSuccess);
            } catch (Exception e) {
                executors.mainThread().execute(onError);
            }
        });
    }
}