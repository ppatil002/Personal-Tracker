package com.example.pp.core.orchestrator.sleep;

import static com.example.pp.core.constants.AppConstants.SLEEP_TARGET_HOUR;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.RemoteInput;

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
        } else if (SleepNotificationHelper.ACTION_DELAY_REASON.equals(action)) {
            handleDelayReason(context, intent);
        } else if (AppConstants.ACTION_SLEEP_ALARM.equals(action)) {
            handleAlarmTrigger(context, reason);
        }
    }

    private void handleStartSleep(Context context) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
            long now = System.currentTimeMillis();
            long targetWakeTime = sleepRepo.getLatestTargetWakeTime();

            sleepRepo.startSleep(now, targetWakeTime);
            SleepNotificationHelper.cancelNotification(context);

            // Update Task State for the Wake-up trigger
            long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(SLEEP_TARGET_HOUR);
            long nextRunTime = calculateNextRunTime("GOOD_NIGHT", bedtime, now);
            updateTaskState(context, nextRunTime);

            // Recalculate will cancel nudges and ensure wake up trigger is set
            SleepScheduler.schedule(context, null);
        });
    }

    private void handleStopSleep(Context context) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
            long now = System.currentTimeMillis();
            sleepRepo.stopSleep(now);
            SleepNotificationHelper.cancelNotification(context);

            // Calculate next day's bedtime for Task State
            long targetWakeTime = sleepRepo.getLatestTargetWakeTime();
            long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(SLEEP_TARGET_HOUR);
            long nextRunTime = calculateNextRunTime("GOOD_MORNING", bedtime, now);
            updateTaskState(context, nextRunTime);

            // Step 3: User accepted Good Morning / Stopped sleep. Reschedule Step 1 alarms for next cycle.
            SleepScheduler.schedule(context, null);
        });
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
                    long targetWakeTime = sleepRepo.getLatestTargetWakeTime();
                    long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(SLEEP_TARGET_HOUR);
                    long nextRunTime = calculateNextRunTime(reason, bedtime, now);
                    updateTaskState(context, nextRunTime);
                } else {
                    // Ignore reminders/nudges if user is already sleeping
                    Log.d(TAG, "User sleeping, ignoring: " + reason);
                }
            } else {
                // User is awake (or pending)
                long targetWakeTime = sleepRepo.getLatestTargetWakeTime();

                long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(AppConstants.SLEEP_TARGET_HOUR);
                
                if (now < bedtime) {
                    // Reminder
                    long diff = bedtime - now;
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                    String msg = "Only " + minutes + "m left until bedtime.";
//                    If it's 2 hours before bedtime, minutes <= 30 is false, so the "Good Night" button is hidden. We only want to show it when it's actually close to bedtime.
//                    Reminders: isOngoing = false, meaning they can be dismissed (swiped away) by the user.
                    SleepNotificationHelper.showBedtimeReminder(context, msg, minutes <= 30, true, false);
                    
                    long nextRunTime = calculateNextRunTime(reason, bedtime, now);
                    updateTaskState(context, nextRunTime);
                } else {
                    // Overdue / nudge
                    long potentialMillis = Math.max(0, targetWakeTime - now);
                    int potentialPercent = (int) (potentialMillis * 100 / TimeUnit.HOURS.toMillis(AppConstants.SLEEP_TARGET_HOUR));
                    
                    String msg;
                    if (reason.contains("target")) {
                        msg = "It's Bedtime! Your current potential is " + potentialPercent + "%.";
                    } else {
                        msg = "Still awake? Time's slipping! Current potential: " + potentialPercent + "%";
                    }
//                   isOngoing = true, making the notification persistent. It will remain in the notification drawer until the "Good Night" button is pressed (which calls cancelNotification)
                    SleepNotificationHelper.showBedtimeReminder(context, msg, true, true, true);
                    
                    long nextRunTime = calculateNextRunTime(reason, bedtime, now);
                    updateTaskState(context, nextRunTime);
                    
                    // Schedule next nudge in a fixed 30m slot from bedtime
                    TaskRepository taskRepo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
                    TaskDefinition def = taskRepo.getDefinitionByType(TaskType.SLEEP_REMINDER);
                    if (def != null) {
                        long interval = TimeUnit.MINUTES.toMillis(30);
                        long elapsedSinceBedtime = Math.max(0, now - bedtime);
                        long nextNudgeOffset = ((elapsedSinceBedtime / interval) + 1) * interval;
                        long nudgeTime = bedtime + nextNudgeOffset;
                        String nudgeReason = "Bedtime nudge @ " + timeFormat.format(new Date(nudgeTime));

                        SleepScheduler.logOperation(context, "SCHEDULE", nudgeReason, "Setting next nudge slot");
                        scheduleNudge(context, nudgeTime, def.getTaskId(), nudgeReason);
                    }
                }
            }
        });
    }

    private void handleDelayReason(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            CharSequence reason = remoteInput.getCharSequence(SleepNotificationHelper.KEY_TEXT_REPLY);
            if (reason != null) {
                String reasonStr = reason.toString().trim();
                SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
                sleepRepo.updateDelayReason(System.currentTimeMillis(), reasonStr);
                
                // Update notification to show success
                SleepNotificationHelper.showBedtimeReminder(context, "Reason saved: " + reasonStr, true, false, true);
            }
        }
    }

    private void scheduleNudge(Context context, long time, String taskId, String reason) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SleepReceiver.class);
        intent.setAction(AppConstants.ACTION_SLEEP_ALARM);
        intent.putExtra("TASK_ID", taskId);
        intent.putExtra("REASON", reason);
        
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
            // Calculate next fixed nudge slot for Task State consistency
            long interval = TimeUnit.MINUTES.toMillis(30);
            long elapsedSinceBedtime = Math.max(0, now - bedtime);
            long nextNudgeOffset = ((elapsedSinceBedtime / interval) + 1) * interval;
            return bedtime + nextNudgeOffset;
        } else if (reason.contains("Wake up trigger") || reason.contains("GOOD_MORNING")){
            // After waking up, the next expected run is the start of the next cycle (2H reminder)
            return bedtime - TimeUnit.HOURS.toMillis(2);
        } else if (reason.contains("GOOD_NIGHT")) {
            // After starting sleep, the next expected run is the wake-up trigger (TargetWake - 1H)
            // Note: targetWake = bedtime + SLEEP_TARGET_HOUR
            return bedtime + TimeUnit.HOURS.toMillis(SLEEP_TARGET_HOUR - 1);
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
