package com.example.pp.core.orchestrator.quotes;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.TaskDefinition;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class QuoteScheduler {
    private static final String TAG = "QuoteScheduler";

    public static void schedule(Context context, TaskDefinition def) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            TaskDefinition taskDef = def;
            if (taskDef == null) {
                taskDef = ((PPApplication) context.getApplicationContext()).getTaskRepository().getDefinitionByType(com.example.pp.core.enums.TaskType.QUOTES);
            }

            if (taskDef != null && !taskDef.isEnabled()) {
                Log.i(TAG, "Quote sync is disabled. Skipping schedule.");
                return;
            }

            Log.i(TAG, "Scheduling Daily Quotes via WorkManager (Target: 6:15 AM)");

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            // Calculate initial delay to 6:15 AM
            long initialDelay = calculateInitialDelay(6, 15);

            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    QuoteWorker.class,
                    24, TimeUnit.HOURS
            )
                    .setConstraints(constraints)
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag(AppConstants.QUOTES_WORKER)
                    .build();

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "DAILY_QUOTE_SYNC",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            );
        });
    }

    public static void cancel(Context context) {
        Log.d(TAG, "Cancelling Quote Sync");
        WorkManager.getInstance(context).cancelUniqueWork("DAILY_QUOTE_SYNC");
    }

    private static long calculateInitialDelay(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1);
        }
        return target.getTimeInMillis() - now.getTimeInMillis();
    }
}
