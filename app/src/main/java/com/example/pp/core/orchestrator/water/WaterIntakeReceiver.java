package com.example.pp.core.orchestrator.water;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.enums.TaskType;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.repository.HydrationRepository;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.core.task.AppExecutors;

public class WaterIntakeReceiver extends BroadcastReceiver {
    private static final String TAG = "WaterIntakeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra("TASK_ID");
        String action = intent.getAction();
        Log.i(TAG, "Water alarm triggered. Action: " + action + ", TaskId: " + taskId);

        if (action == null) {
            // Regular alarm trigger
            WaterNotificationHelper.showNotification(context, taskId);
            updateTaskLastRun(context, taskId);
        } else {
            handleAction(context, action, taskId);
        }
    }

    private void updateTaskLastRun(Context context, String taskId) {
        if (taskId == null) return;
        AppExecutors.getInstance().diskIO().execute(() -> {
            TaskRepository repo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
            TaskState state = repo.getStateForTask(taskId);
            if (state != null) {
                state.setLastRunTime(System.currentTimeMillis());
                repo.updateState(state);
            }
        });
    }

    private void handleAction(Context context, String action, String taskId) {
        TaskRepository taskRepo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        HydrationRepository hydrationRepo = new HydrationRepository(context);
        AppExecutors executors = AppExecutors.getInstance();

        executors.diskIO().execute(() -> {
            TaskDefinition def = taskRepo.getDefinitionByType(TaskType.WATER_REMINDER);
            if (def == null) return;

            if (WaterNotificationHelper.ACTION_100ML.equals(action)) {
                hydrationRepo.addIntakeSync(100, System.currentTimeMillis());
                WaterNotificationHelper.cancelNotification(context);
                WaterIntakeScheduler.schedule(context, def);
            } else if (WaterNotificationHelper.ACTION_250ML.equals(action)) {
                hydrationRepo.addIntakeSync(250, System.currentTimeMillis());
                WaterNotificationHelper.cancelNotification(context);
                WaterIntakeScheduler.schedule(context, def);
            } else if (WaterNotificationHelper.ACTION_SNOOZE.equals(action)) {
                WaterNotificationHelper.cancelNotification(context);
                WaterIntakeScheduler.snooze(context, def);
            }
        });
    }
}
