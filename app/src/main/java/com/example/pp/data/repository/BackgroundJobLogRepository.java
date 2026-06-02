package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.BackgroundJobLogDao;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

public class BackgroundJobLogRepository {
    private final BackgroundJobLogDao dao;
    private final AppExecutors executors;

    public BackgroundJobLogRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).backgroundJobLogDao();
        this.executors = AppExecutors.getInstance();
    }

    public void mergeDuplicateDailyWorkerLog(BackgroundJobLog log) {
        executors.diskIO().execute(() -> {
            int updatedRows = dao.updateExistingDailyLog(
                    log.getWorkerName(),
                    log.getCurrentStep(),
                    log.getStatus(),
                    log.getLogMessage(),
                    log.getLastUpdatedTimestamp()
            );

            if (updatedRows == 0) {
                dao.insert(log);
            }
        });
    }

    public void insert(BackgroundJobLog log) {
        executors.diskIO().execute(() -> {
            dao.insert(log);
        });
    }

    public LiveData<List<BackgroundJobLog>> getAllLogs() {
        return dao.getAllLogs();
    }

    public List<BackgroundJobLog> getAllLogsList() {
        return dao.getAllLogsList();
    }

    public SyncStats upsertAll(List<BackgroundJobLog> logs) {
        return dao.upsertAll(logs);
    }
}
