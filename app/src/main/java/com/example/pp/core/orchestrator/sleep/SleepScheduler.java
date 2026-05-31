package com.example.pp.core.orchestrator.sleep;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.TaskStatus;
import com.example.pp.core.enums.TaskType;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.SleepEntity;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.repository.SleepRepository;
import com.example.pp.data.repository.TaskRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class SleepScheduler {
    private static final String TAG = "SleepScheduler";

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
        SleepRepository sleepRepo = new SleepRepository(context);
        SleepEntity lastSleep = sleepRepo.getLastSleepSync();
        
        long now = System.currentTimeMillis();
        long targetWakeTime;
        
        // 1. Determine Target Wake Time
        if (lastSleep != null && lastSleep.targetWakeTime > now - TimeUnit.HOURS.toMillis(12)) {
            // Use the target wake time from the latest entry if it's within a reasonable window
            targetWakeTime = lastSleep.targetWakeTime;
        } else {
            // Default to 6 AM today or tomorrow
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 6);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= now) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            targetWakeTime = cal.getTimeInMillis();
        }

        long bedtime = targetWakeTime - TimeUnit.HOURS.toMillis(8);
        long nextAlarm = 0;
        String reason = "";

        // 2. Determine Next Alarm Time
        if (lastSleep != null && lastSleep.startTime != 0 && lastSleep.endTime == 0) {
            // CASE A: User is currently sleeping. Next event is the Wake Up trigger.
            if (now < targetWakeTime) {
                nextAlarm = targetWakeTime;
                reason = "Wake up trigger";
            } else {
                // Already past wake time but haven't "woken up" in app. 
                SleepNotificationHelper.showWakeUpNotification(context);
                return;
            }
        } else {
            // CASE B: User is awake. Check for bedtime reminders.
            long[] reminderOffsets = {
                TimeUnit.HOURS.toMillis(2),
                TimeUnit.HOURS.toMillis(1),
                TimeUnit.MINUTES.toMillis(30),
                TimeUnit.MINUTES.toMillis(10),
                0 // Exactly at bedtime
            };

            for (long offset : reminderOffsets) {
                long reminderTime = bedtime - offset;
                if (reminderTime > now) {
                    nextAlarm = reminderTime;
                    reason = "Bedtime reminder (" + (offset / (60 * 1000)) + "m before)";
                    break;
                }
            }

            if (nextAlarm == 0 && now >= bedtime) {
                // Already past bedtime but hasn't started sleep. Nag every 30 mins.
                nextAlarm = now + TimeUnit.MINUTES.toMillis(30);
                reason = "Bedtime nag reminder";
            }
        }

        // 3. Set the Alarm and Update State
        if (nextAlarm > 0) {
            setNativeAlarm(context, nextAlarm, taskId);
            updateTaskState(context, taskId, nextAlarm);
            logOperation(context, "SCHEDULED", reason, "Next alarm at: " + new Date(nextAlarm));
        }
    }

    private static void setNativeAlarm(Context context, long timeMillis, String taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, SleepReceiver.class);
        intent.putExtra("TASK_ID", taskId);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                AppConstants.RC_SLEEP_ALARM, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent); // Always cancel old one
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
            Log.i(TAG, "Native alarm set for: " + new Date(timeMillis));
        }
    }

    private static void updateTaskState(Context context, String taskId, long nextRunTime) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            TaskRepository repo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
            TaskState state = repo.getStateForTask(taskId);
            if (state != null) {
                state.setNextRunTime(nextRunTime);
                state.setLastRunTime(System.currentTimeMillis());
                state.setCurrentState(TaskStatus.SCHEDULED);
                repo.updateState(state);
            }
        });
    }

    private static void logOperation(Context context, String status, String step, String message) {
        BackgroundJobLog log = new BackgroundJobLog(
                "SLEEP_" + System.currentTimeMillis(),
                TAG,
                status,
                step,
                message,
                System.currentTimeMillis()
        );
        AppExecutors.getInstance().diskIO().execute(() -> {
            AppDatabase.getInstance(context).backgroundJobLogDao().insert(log);
        });
    }
}
