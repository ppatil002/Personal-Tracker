package com.example.pp.core.orchestrator.sleep;

import static com.example.pp.core.constants.AppConstants.SLEEP_TARGET_HOUR;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SleepReceiver extends BroadcastReceiver {
    private static final String TAG = "SleepReceiver";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String reason = intent.getStringExtra("REASON");
        if (reason == null) reason = "Unknown trigger";
        
        Log.i(TAG, "onReceive: " + action + " | Reason: " + reason);
        SleepScheduler.logOperation(context, "SUCCESS", reason, "Action: " + action + " | Reason: " + reason);

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
        long now = System.currentTimeMillis();
        
        long targetWakeTime = sleepRepo.getTargetWakeTime(now);

        sleepRepo.startSleep(now, targetWakeTime);
        SleepNotificationHelper.cancelNotification(context);
        // Recalculate will cancel nudges and ensure wake up trigger is set
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
                    long targetWakeTime = sleepRepo.getTargetWakeTime(now);
                    long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(SLEEP_TARGET_HOUR);
                    long nextRunTime = calculateNextRunTime(reason, bedtime, now);
                    updateTaskState(context, nextRunTime);
                } else {
                    // Ignore reminders/nudges if user is already sleeping
                    Log.d(TAG, "User sleeping, ignoring: " + reason);
                }
            } else {
                // User is awake (or pending)
                long targetWakeTime = sleepRepo.getTargetWakeTime(now);

                long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(AppConstants.SLEEP_TARGET_HOUR);
                
                if (now < bedtime) {
                    // Reminder
                    long diff = bedtime - now;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                    String msg = "Only " + minutes + "m left until bedtime.";
                    SleepNotificationHelper.showBedtimeReminder(context, msg, minutes <= 30);
                    
                    long nextRunTime = calculateNextRunTime(reason, bedtime, now);
                    updateTaskState(context, nextRunTime);
                } else {
                    // Overdue / nudge
                    long potentialMillis = Math.max(0, targetWakeTime - now);
                    int potentialPercent = (int) (potentialMillis * 100 / TimeUnit.HOURS.toMillis(AppConstants.SLEEP_TARGET_HOUR));
                    String msg = "Time's up! Current potential: " + potentialPercent + "%";
                    SleepNotificationHelper.showBedtimeReminder(context, msg, true);
                    
                    long nextRunTime = calculateNextRunTime(reason, bedtime, now);
                    updateTaskState(context, nextRunTime);
                    
                    // Schedule next nudge in 30 mins
                    TaskRepository taskRepo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
                    TaskDefinition def = taskRepo.getDefinitionByType(TaskType.SLEEP_REMINDER);
                    if (def != null) {
                        SleepScheduler.logOperation(context, "SCHEDULE", "Bedtime nudge @ "+ timeFormat.format(new Date(now + TimeUnit.MINUTES.toMillis(30))), "Setting next nudge in 30m");
                        scheduleNudge(context, now + TimeUnit.MINUTES.toMillis(30), def.getTaskId());
                    }
                }
            }
        });
    }

    private void scheduleNudge(Context context, long time, String taskId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SleepReceiver.class);
        intent.setAction(AppConstants.ACTION_SLEEP_ALARM);
        intent.putExtra("TASK_ID", taskId);
        intent.putExtra("REASON", "Bedtime nudge (30m)");
        
        PendingIntent pi = PendingIntent.getBroadcast(
                context, 
                AppConstants.RC_SLEEP_NUDGE,
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        if (am != null) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi);
        }
    }

    private long calculateNextRunTime(String reason, long bedtime, long now) {
        if (reason.contains("2H")) {
            return bedtime - TimeUnit.HOURS.toMillis(1);
        } else if (reason.contains("1H")) {
            return bedtime - TimeUnit.MINUTES.toMillis(30);
        } else if (reason.contains("30m")) {
            return bedtime - TimeUnit.MINUTES.toMillis(10);
        } else if (reason.contains("10m")) {
            return bedtime;
        } else if (reason.contains("target") || reason.contains("nudge")) {
            return now + TimeUnit.MINUTES.toMillis(30);
        } else if (reason.contains("Wake up trigger")){
            // Setting for next day's time
            return bedtime - TimeUnit.HOURS.toMillis(2);
        }
        return 0;
    }

    private void updateTaskState(Context context, long nextRunTime) {
        TaskRepository repo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        TaskDefinition def = repo.getDefinitionByType(TaskType.SLEEP_REMINDER);
        if (def != null) {
            TaskState state = repo.getStateForTask(def.getTaskId());
            if (state != null) {
                state.setLastRunTime(System.currentTimeMillis());
                if (nextRunTime > 0) {
                    state.setNextRunTime(nextRunTime);
                }
                repo.updateState(state);
            }
        }
    }
}
