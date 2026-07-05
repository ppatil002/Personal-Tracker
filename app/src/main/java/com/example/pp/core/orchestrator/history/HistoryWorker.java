package com.example.pp.core.orchestrator.history;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.example.pp.PPApplication;
import com.example.pp.core.orchestrator.BaseFeatureWorker;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.repository.BackgroundJobLogRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryWorker extends BaseFeatureWorker {
    private static final String TAG = "HistoryWorker";

    public HistoryWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting HistoryWorker (Daily History Fetch)");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String customJobId = "history_" + today;
        
        PPApplication app = (PPApplication) getApplicationContext();
        BackgroundJobLogRepository logRepo = app.getBackgroundJobLogRepository();

        logRepo.mergeDuplicateDailyWorkerLog(new BackgroundJobLog(
                customJobId,
                "HistoryFetchWorker",
                "RUNNING",
                "Daily History",
                "Worker triggered fetch for " + today,
                System.currentTimeMillis()
        ));

        try {
            app.getHistoryRepository().fetchHistoryOfDay(today);
            completeTask();
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in HistoryWorker", e);
            logRepo.mergeDuplicateDailyWorkerLog(new BackgroundJobLog(
                    customJobId,
                    "HistoryFetchWorker",
                    "FAILED",
                    "Daily History",
                    "Worker Error: " + e.getMessage(),
                    System.currentTimeMillis()
            ));
            failTask(e.getMessage());
            return Result.failure();
        }
    }
}
