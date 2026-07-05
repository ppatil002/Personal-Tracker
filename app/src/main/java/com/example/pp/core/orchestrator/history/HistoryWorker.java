package com.example.pp.core.orchestrator.history;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.example.pp.PPApplication;
import com.example.pp.core.orchestrator.BaseFeatureWorker;

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
        String dailyJobId = "history_" + today;
        
        PPApplication app = (PPApplication) getApplicationContext();

        mergeUpdateTodayLog(dailyJobId, "RUNNING", "Daily History", "Worker triggered fetch for " + today);

        try {
            app.getHistoryRepository().fetchHistoryOfDay(today);
            completeTask();
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in HistoryWorker", e);
            mergeUpdateTodayLog(dailyJobId, "FAILED", "Daily History", "Worker Error: " + e.getMessage());
            failTask(e.getMessage());
            return Result.failure();
        }
    }
}
