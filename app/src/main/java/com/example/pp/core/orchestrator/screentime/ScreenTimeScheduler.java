package com.example.pp.core.orchestrator.screentime;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.TaskType;
import com.example.pp.core.preferences.PreferenceManager;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.features.screentime.service.ScreenTrackingService;

import java.util.concurrent.TimeUnit;

public class ScreenTimeScheduler {
    private static final String TAG = "ScreenTimeScheduler";

    public static void schedule(Context context, @Nullable TaskDefinition def) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            TaskDefinition taskDef = def;
            if (taskDef == null) {
                taskDef = ((PPApplication) context.getApplicationContext()).getTaskRepository().getDefinitionByType(com.example.pp.core.enums.TaskType.SCREEN_TIME_SYNC);
            }

            if (taskDef != null && !taskDef.isEnabled()) {
                Log.i(TAG, "Screen time tracking is disabled. Skipping schedule.");
                return;
            }

            Log.d(TAG, "Scheduling Screen Time Sync Fallback");

            // Enable tracking in preferences
            PreferenceManager.getInstance(context).putBoolean(AppConstants.KEY_SCREEN_TIME_ENABLED, true);

            // Notify the running service to update its state and resume activities
            notifyService(context);

            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(false)
                    .build();

            long interval = (taskDef != null && taskDef.getRepeatInterval() > 0) ? taskDef.getRepeatInterval() : 15;
            String taskId = (taskDef != null) ? taskDef.getTaskId() : "SCREENTIME_TASK_001";

            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    ScreenTimeSyncWorker.class,
                    interval,
                    TimeUnit.MINUTES
            )
                    .setConstraints(constraints)
                    .addTag(TaskType.SCREEN_TIME_SYNC.name())
                    .addTag(taskId)
                    .build();

            // Unique Periodic Work ensures only one fallback loop exists
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "SCREEN_TIME_SYNC_PERIODIC",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
            );
        });
    }

    public static void cancel(Context context) {
        Log.d(TAG, "Cancelling Screen Time Sync");
        
        // Disable tracking in preferences
        PreferenceManager.getInstance(context).putBoolean(AppConstants.KEY_SCREEN_TIME_ENABLED, false);

        // Notify the running service to update its state and stop activities
        notifyService(context);

        WorkManager.getInstance(context).cancelUniqueWork("SCREEN_TIME_SYNC_PERIODIC");
        WorkManager.getInstance(context).cancelAllWorkByTag(TaskType.SCREEN_TIME_SYNC.name());
    }

    private static void notifyService(Context context) {
        Intent intent = new Intent(context, ScreenTrackingService.class);
        intent.setAction("UPDATE_STATE");
        context.startService(intent);
    }

    public static void runNow(Context context, String taskId) {
        Log.d(TAG, "Triggering Immediate Screen Time Sync for task: " + taskId);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ScreenTimeSyncWorker.class)
                .addTag(TaskType.SCREEN_TIME_SYNC.name())
                .addTag(taskId)
                .build();

        // Unique Work ensures that rapid lock/unlock events don't spawn multiple workers
        WorkManager.getInstance(context).enqueueUniqueWork(
                "SCREEN_TIME_SYNC_NOW",
                ExistingWorkPolicy.REPLACE,
                request
        );
    }
}
