package com.example.pp.core.orchestrator.quotes;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.example.pp.PPApplication;
import com.example.pp.core.orchestrator.BaseFeatureWorker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class QuoteWorker extends BaseFeatureWorker {
    private static final String TAG = "QuoteWorker";

    public QuoteWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting QuoteWorker (Daily Quote Fetch)");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String dailyJobId = "quotes_" + today;
        
        PPApplication app = (PPApplication) getApplicationContext();

        mergeUpdateTodayLog(dailyJobId, "RUNNING", "Daily Quote", "Worker triggered fetch for " + today);

        try {
            app.getQuoteRepository().fetchQuoteOfTheDay(today);
            completeTask();
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in QuoteWorker", e);
            mergeUpdateTodayLog(dailyJobId, "FAILED", "Daily Quote", "Worker Error: " + e.getMessage());
            failTask(e.getMessage());
            return Result.failure();
        }
    }
}
