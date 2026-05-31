package com.example.pp.core.orchestrator.screentime;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.pp.core.enums.TaskType;
import com.example.pp.data.local.entity.TaskDefinition;

import java.util.concurrent.TimeUnit;

public class ScreenTimeScheduler {
    private static final String TAG = "ScreenTimeScheduler PP";

    public static void schedule(Context context, TaskDefinition def) {
        Log.d(TAG, "Scheduling Screen Time Sync: " + def.getTaskId());

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ScreenTimeSyncWorker.class,
                def.getRepeatInterval() > 0 ? def.getRepeatInterval() : 15, // Default 15 mins if not set
                TimeUnit.MINUTES
        )
                .setConstraints(constraints)
                .addTag(TaskType.SCREEN_TIME_SYNC.name())
                .addTag(def.getTaskId())
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SCREEN_TIME_SYNC_JOB",
                ExistingPeriodicWorkPolicy.REPLACE,
                request
        );
    }

    public static void runNow(Context context, String taskId) {
        Log.d(TAG, "Forcing immediate Screen Time Sync for task: " + taskId);
        androidx.work.OneTimeWorkRequest request = new androidx.work.OneTimeWorkRequest.Builder(ScreenTimeSyncWorker.class)
                .addTag(TaskType.SCREEN_TIME_SYNC.name())
                .addTag(taskId)
                .build();
        WorkManager.getInstance(context).enqueue(request);
    }
}
