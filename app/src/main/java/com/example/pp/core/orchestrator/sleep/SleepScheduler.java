package com.example.pp.core.orchestrator.sleep;

import static com.example.pp.core.constants.AppConstants.SLEEP_TARGET_HOUR;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.TaskType;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.repository.SleepRepository;
import com.example.pp.data.repository.TaskRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SleepScheduler {
    private static final String TAG = "SleepScheduler";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    /**
     * Entry point for the Task Engine / SystemTaskScheduler.
     */
    public static void schedule(Context context, TaskDefinition def) {
        Log.i(TAG, "Scheduling Sleep Monitor task.");
        calculateAndSetNextAlarm(context, def.getTaskId());
    }

    /**
     * Force a recalculation and rescheduling of the next sleep-related alarm.
     */
    public static void reschedule(Context context) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            TaskRepository taskRepo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
            TaskDefinition def = taskRepo.getDefinitionByType(TaskType.SLEEP_REMINDER);
            if (def != null) {
                calculateAndSetNextAlarm(context, def.getTaskId());
            } else {
                Log.e(TAG, "Cannot reschedule: Sleep Monitor task definition not found.");
            }
        });
    }

    private static void calculateAndSetNextAlarm(Context context, String taskId) {
        SleepRepository sleepRepo = ((PPApplication) context.getApplicationContext()).getSleepRepository();
        SleepEntity lastSleep = sleepRepo.getLastSleepSync();
        
        long now = System.currentTimeMillis();
        long targetWakeTime = sleepRepo.getTargetWakeTime(now);
        long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(SLEEP_TARGET_HOUR);

        if (lastSleep != null && lastSleep.startTime != 0 && lastSleep.endTime == 0) {
            // CASE: User is currently sleeping.
            // Ensure Wake Up trigger is set.
            long wakeTriggerTime = targetWakeTime - TimeUnit.HOURS.toMillis(1);
            if (wakeTriggerTime > now) {
                setNativeAlarm(context, wakeTriggerTime, taskId, AppConstants.RC_SLEEP_WAKEUP, "Wake up trigger @" + timeFormat.format(new Date(wakeTriggerTime)));
            }
            
            // Cancel reminders/nudges as they are no longer needed
            cancelAlarm(context, AppConstants.RC_SLEEP_TARGET);
            cancelAlarm(context, AppConstants.RC_SLEEP_NUDGE);
            cancelAlarm(context, AppConstants.RC_SLEEP_REMINDER_2H);
            cancelAlarm(context, AppConstants.RC_SLEEP_REMINDER_1H);
            cancelAlarm(context, AppConstants.RC_SLEEP_REMINDER_30M);
            cancelAlarm(context, AppConstants.RC_SLEEP_REMINDER_10M);
        } else {
            // CASE: User is awake (startTime == 0).
            // 1. Schedule the sequence of reminders (Step 1)
            scheduleIfFuture(context, taskId, bedtime - TimeUnit.HOURS.toMillis(2), AppConstants.RC_SLEEP_REMINDER_2H, "2H reminder @ "+ timeFormat.format(new Date(bedtime - TimeUnit.HOURS.toMillis(2))));
            scheduleIfFuture(context, taskId, bedtime - TimeUnit.HOURS.toMillis(1), AppConstants.RC_SLEEP_REMINDER_1H, "1H reminder @ "+ timeFormat.format(new Date(bedtime - TimeUnit.HOURS.toMillis(1))));
            scheduleIfFuture(context, taskId, bedtime - TimeUnit.MINUTES.toMillis(30), AppConstants.RC_SLEEP_REMINDER_30M, "30m reminder @ "+ timeFormat.format(new Date(bedtime - TimeUnit.MINUTES.toMillis(30))));
            scheduleIfFuture(context, taskId, bedtime - TimeUnit.MINUTES.toMillis(10), AppConstants.RC_SLEEP_REMINDER_10M, "10m reminder @ "+ timeFormat.format(new Date(bedtime - TimeUnit.MINUTES.toMillis(10))));
            scheduleIfFuture(context, taskId, bedtime, AppConstants.RC_SLEEP_TARGET, "Bedtime target @ "+ timeFormat.format(new Date(bedtime)));
            
            // Wake up trigger (waketime - 1 hour)
            scheduleIfFuture(context, taskId, targetWakeTime - TimeUnit.HOURS.toMillis(1), AppConstants.RC_SLEEP_WAKEUP, "Wake up trigger @ "+ timeFormat.format(new Date(targetWakeTime - TimeUnit.HOURS.toMillis(1))));

            if (now < bedtime) {
                cancelAlarm(context, AppConstants.RC_SLEEP_NUDGE);
            }
        }
    }

    private static void scheduleIfFuture(Context context, String taskId, long time, int rc, String reason) {
        if (time > System.currentTimeMillis()) {
            setNativeAlarm(context, time, taskId, rc, reason);
        } else {
            Log.d(TAG, "Skipping "+ reason + " Alarm cause time exceeded");
        }
    }

    private static void setNativeAlarm(Context context, long timeMillis, String taskId, int requestCode, String reason) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SleepReceiver.class);
        intent.setAction(AppConstants.ACTION_SLEEP_ALARM);
        intent.putExtra("TASK_ID", taskId);
        intent.putExtra("REASON", reason);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
            Log.i(TAG, "Alarm set: " + reason + " for " + new Date(timeMillis));
            logOperation(context, "SCHEDULE", reason, "Alarm set for " + new Date(timeMillis));
        }
    }

    public static void cancelAlarm(Context context, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SleepReceiver.class);
        intent.setAction(AppConstants.ACTION_SLEEP_ALARM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                requestCode, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    public static void logOperation(Context context, String status, String step, String message) {
        BackgroundJobLog log = new BackgroundJobLog(
                "SLEEP_" + System.currentTimeMillis(),
                TAG,
                status,
                step,
                message,
                System.currentTimeMillis()
        );
        AppExecutors.getInstance().diskIO().execute(() -> {
            ((PPApplication) context.getApplicationContext())
                .getBackgroundJobLogRepository()
                .mergeDuplicateDailyWorkerLog(log);
        });
    }
}
