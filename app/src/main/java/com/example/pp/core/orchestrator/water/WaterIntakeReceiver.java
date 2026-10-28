package com.example.pp.core.orchestrator.water;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.TaskType;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.repository.HydrationRepository;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.core.task.AppExecutors;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class WaterIntakeReceiver extends BroadcastReceiver {
    private static final String TAG = "WaterIntakeReceiver";

    private static HydrationRepository getHydrationRepository(Context context) {
        Context app = context.getApplicationContext();
        if (app instanceof PPApplication) {
            return ((PPApplication) app).getHydrationRepository();
        }
        return new HydrationRepository(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String taskId = intent.getStringExtra("TASK_ID");
        String action = intent.getAction();
        Log.i(TAG, "Water alarm triggered. Action: " + action + ", TaskId: " + taskId);

        if (AppConstants.ACTION_WATER_ALARM.equals(action)) {
            processAlarm(context, taskId);
        } else {
            handleAction(context, action, taskId);
        }
    }

    private void processAlarm(Context context, String taskId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            HydrationRepository hydrationRepository = getHydrationRepository(context);
            HydrationIntake lastIntake = hydrationRepository.findLatestIntake();
            long now = System.currentTimeMillis();

            // Handle race condition: if user just logged water, skip the notification
            if (lastIntake != null && (now - lastIntake.getTimestamp()) < TimeUnit.HOURS.toMillis(2) - 10000) {
                Log.i(TAG, "Recent intake found. Skipping notification and rescheduling.");
                WaterIntakeScheduler.schedule(context, null);
                return;
            }

            // Window check: 11 AM to 11 PM
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (hour < 11 || hour >= 23) {
                Log.i(TAG, "Alarm triggered outside window (11-23). Rescheduling.");
                WaterIntakeScheduler.schedule(context, null);
                return;
            }

            long lastIntakeTime = lastIntake != null ? lastIntake.getTimestamp() : 0L;
            int lastIntakeAmount = lastIntake != null ? lastIntake.getAmountMl() : 0;
            WaterNotificationHelper.showNotification(context, taskId, lastIntakeTime, lastIntakeAmount);
            updateTaskLastRun(context, taskId);

            // Schedule a repeat reminder after 20 minutes if no action taken
            WaterIntakeScheduler.scheduleReminderRepeat(context, taskId);
        });
    }

    private void updateTaskLastRun(Context context, String taskId) {
        if (taskId == null) return;
        TaskRepository repo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        TaskState state = repo.getStateForTask(taskId);
        if (state != null) {
            state.setLastRunTime(System.currentTimeMillis());
            repo.updateState(state);
        }
    }

    private void handleAction(Context context, String action, String taskId) {
        TaskRepository taskRepo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        HydrationRepository hydrationRepository = getHydrationRepository(context);
        AppExecutors executors = AppExecutors.getInstance();

        executors.diskIO().execute(() -> {
            TaskDefinition def = taskRepo.getDefinitionByType(TaskType.WATER_REMINDER);
            if (def == null) return;

            if (WaterNotificationHelper.ACTION_300ML.equals(action)) {
                hydrationRepository.addIntakeSync(300, System.currentTimeMillis());
                WaterNotificationHelper.cancelNotification(context);
                WaterIntakeScheduler.schedule(context, def);
            } else if (WaterNotificationHelper.ACTION_250ML.equals(action)) {
                hydrationRepository.addIntakeSync(250, System.currentTimeMillis());
                WaterNotificationHelper.cancelNotification(context);
                WaterIntakeScheduler.schedule(context, def);
            } else if (WaterNotificationHelper.ACTION_SNOOZE.equals(action)) {
                WaterNotificationHelper.cancelNotification(context);
                WaterIntakeScheduler.snooze(context, def);
            }
        });
    }
}
