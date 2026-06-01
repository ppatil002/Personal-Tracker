package com.example.pp.core.orchestrator.sleep;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.TaskType;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.repository.SleepRepository;
import com.example.pp.data.repository.TaskRepository;

import java.util.concurrent.TimeUnit;

public class SleepReceiver extends BroadcastReceiver {
    private static final String TAG = "SleepReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String reason = intent.getStringExtra("REASON");
        if (reason == null) reason = "Unknown trigger";
        
        Log.i(TAG, "onReceive: " + action + " | Reason: " + reason);
        SleepScheduler.logOperation(context, "EXECUTE", "Alarm Received", "Action: " + action + " | Reason: " + reason);

        if (SleepNotificationHelper.ACTION_START_SLEEP.equals(action)) {
            handleStartSleep(context);
        } else if (SleepNotificationHelper.ACTION_STOP_SLEEP.equals(action)) {
            handleStopSleep(context);
        } else if (AppConstants.ACTION_SLEEP_ALARM.equals(action)) {
            handleAlarmTrigger(context, reason);
        }
    }

    private void handleStartSleep(Context context) {
        SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
        SleepEntity lastSleep = sleepRepo.getLastSleepSync();
        long now = System.currentTimeMillis();
        
        long targetWakeTime = sleepRepo.getTargetWakeTime(lastSleep, now);

        sleepRepo.startSleep(now, targetWakeTime);
        SleepNotificationHelper.cancelNotification(context);
        // Recalculate will cancel nags and ensure wake up trigger is set
        SleepScheduler.reschedule(context);
    }

    private void handleStopSleep(Context context) {
        SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
        sleepRepo.stopSleep(System.currentTimeMillis());
        SleepNotificationHelper.cancelNotification(context);
        // Step 3: User accepted Good Morning / Stopped sleep. Reschedule Step 1 alarms for next cycle.
        SleepScheduler.reschedule(context);
    }

    private void handleAlarmTrigger(Context context, String reason) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
            SleepEntity lastSleep = sleepRepo.getLastSleepSync();
            long now = System.currentTimeMillis();

            if (lastSleep != null && lastSleep.startTime != 0 && lastSleep.endTime == 0) {
                // User is sleeping
                if (reason.contains("Wake up trigger")) {
                    SleepNotificationHelper.showWakeUpNotification(context);
                    updateTaskLastRun(context);
                } else {
                    // Ignore reminders/nags if user is already sleeping
                    Log.d(TAG, "User sleeping, ignoring: " + reason);
                }
            } else {
                // User is awake (or pending)
                long targetWakeTime = sleepRepo.getTargetWakeTime(lastSleep, now);

                long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(AppConstants.SLEEP_TARGET_HOUR);
                
                if (now < bedtime) {
                    // Reminder
                    long diff = bedtime - now;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                    String msg = "Only " + minutes + "m left until bedtime.";
                    SleepNotificationHelper.showBedtimeReminder(context, msg, minutes <= 30);
                    updateTaskLastRun(context);
                } else {
                    // Overdue / Nag
                    long potentialMillis = Math.max(0, targetWakeTime - now);
                    int potentialPercent = (int) (potentialMillis * 100 / TimeUnit.HOURS.toMillis(AppConstants.SLEEP_TARGET_HOUR));
                    String msg = "Time's up! Current potential: " + potentialPercent + "%";
                    SleepNotificationHelper.showBedtimeReminder(context, msg, true);
                    updateTaskLastRun(context);
                    
                    // Schedule next nag in 30 mins
                    TaskRepository taskRepo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
                    TaskDefinition def = taskRepo.getDefinitionByType(TaskType.SLEEP_REMINDER);
                    if (def != null) {
                        SleepScheduler.logOperation(context, "SCHEDULE", "Bedtime nag", "Setting next nag in 30m");
                        scheduleNag(context, now + TimeUnit.MINUTES.toMillis(30), def.getTaskId());
                    }
                }
            }
        });
    }

    private void scheduleNag(Context context, long time, String taskId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SleepReceiver.class);
        intent.setAction(AppConstants.ACTION_SLEEP_ALARM);
        intent.putExtra("TASK_ID", taskId);
        intent.putExtra("REASON", "Bedtime nag (30m)");
        
        PendingIntent pi = PendingIntent.getBroadcast(
                context, 
                AppConstants.RC_SLEEP_NAG, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
        }
    }

    private void updateTaskLastRun(Context context) {
        TaskRepository repo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        TaskDefinition def = repo.getDefinitionByType(TaskType.SLEEP_REMINDER);
        if (def != null) {
            TaskState state = repo.getStateForTask(def.getTaskId());
            if (state != null) {
                state.setLastRunTime(System.currentTimeMillis());
                repo.updateState(state);
            }
        }
    }
}
