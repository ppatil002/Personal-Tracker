package com.example.pp.core.orchestrator.water;

import static com.example.pp.core.constants.AppConstants.WATER_WORKER;

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
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.HydrationIntake;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.repository.HydrationRepository;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.utils.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class WaterIntakeScheduler {
    private static final String TAG = "WaterIntakeScheduler";
    public static final int ALARM_REQUEST_CODE = AppConstants.RC_WATER_ALARM;

    /**
     * Reschedules the water intake alarm based on the latest database state.
     */
    public static void schedule(Context context, TaskDefinition def) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            TaskDefinition taskDef = def;
            if (taskDef == null) {
                TaskRepository taskRepository = ((PPApplication) context.getApplicationContext()).getTaskRepository();
                taskDef = taskRepository.getDefinitionByType(TaskType.WATER_REMINDER);
            }

            // Skip if task is disabled to prevent ghost notifications/alarms
            if (taskDef == null || !taskDef.isEnabled()) {
                Log.i(TAG, "Water intake is disabled. Skipping schedule.");
                return;
            }

            Log.i(TAG, "Scheduling water intake alarm.");

            long nextAlarmTime = calculateNextAlarmTime(context);
            
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, WaterIntakeReceiver.class);
            intent.setAction(AppConstants.ACTION_WATER_ALARM);
            intent.putExtra("TASK_ID", taskDef.getTaskId());
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    ALARM_REQUEST_CODE, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                // Cancel any existing alarm before scheduling a new one
                alarmManager.cancel(pendingIntent);
                
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextAlarmTime,
                        pendingIntent
                );
                Log.i(TAG, "Water alarm set for: " + new Date(nextAlarmTime));
                logOperation(context, "SCHEDULE", "Water Alarm", "Alarm set for " + new Date(nextAlarmTime));

                // Update TaskState
                updateTaskState(context, taskDef.getTaskId(), nextAlarmTime);
            }
        });
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, WaterIntakeReceiver.class);
        intent.setAction(AppConstants.ACTION_WATER_ALARM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.i(TAG, "Water intake alarm cancelled.");
            logOperation(context, "CANCEL", "Water Alarm", "Water alarm cancelled manually.");
        }
    }

    /**
     * Logs a water intake and reschedules the alarm sequentially to avoid race conditions.
     */
    public static void logIntakeAndReschedule(Context context, int amountMl, long timestamp) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            HydrationRepository hydrationRepo = new HydrationRepository(context);
            // 1. Log the intake (Synchronously on this background thread)
            hydrationRepo.addIntakeSync(amountMl, timestamp);
            Log.d(TAG, "Logged intake: " + amountMl + "ml at " + timestamp);
            
            // 2. Reschedule
            schedule(context, null);
        });
    }

    private static void updateTaskState(Context context, String taskId, long nextRunTime) {
        TaskRepository repo = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        TaskState state = repo.getStateForTask(taskId);
        if (state != null) {
            state.setNextRunTime(nextRunTime);
            state.setLastRunTime(System.currentTimeMillis());
            state.setCurrentState(TaskStatus.SCHEDULED);
            repo.updateState(state);
        }
    }

    private static long calculateNextAlarmTime(Context context) {
        HydrationRepository repo = new HydrationRepository(context);
        
        Calendar now = Calendar.getInstance();
        long currentMillis = now.getTimeInMillis();
        
        // Start of today
        Calendar startOfDay = (Calendar) now.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        
        // 11 AM today
        Calendar elevenAM = (Calendar) startOfDay.clone();
        elevenAM.set(Calendar.HOUR_OF_DAY, 11);
        
        // 11 PM today
        Calendar elevenPM = (Calendar) startOfDay.clone();
        elevenPM.set(Calendar.HOUR_OF_DAY, 23);
        
        // Get absolute last intake instead of just today
        HydrationIntake lastIntake = repo.findLatestIntake();
        
        long baseTime;
        if (lastIntake == null) {
            baseTime = elevenAM.getTimeInMillis();
        } else {
            // Next run is at least 2 hours after the last drink, but not before 11 AM
            baseTime = Math.max(lastIntake.getTimestamp() + TimeUnit.HOURS.toMillis(2), elevenAM.getTimeInMillis());
        }
        
        // If it's already past 11 PM today, schedule for tomorrow 11 AM
        if (currentMillis >= elevenPM.getTimeInMillis()) {
            return getNextDayStart();
        }

        // If baseTime is past 11 PM, schedule for tomorrow 11 AM
        if (baseTime > elevenPM.getTimeInMillis()) {
            return getNextDayStart();
        }
        
        // If calculated baseTime is in the past, and it's currently between 11 AM and 11 PM
        if (baseTime <= currentMillis) {
            if (currentMillis < elevenAM.getTimeInMillis()) {
                return elevenAM.getTimeInMillis();
            } else {
                return currentMillis + 1000; // Trigger almost immediately
            }
        }
        
        return baseTime;
    }

    private static long getNextDayStart() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 11);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
    
    public static void snooze(Context context, TaskDefinition def) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Log.i(TAG, "Snoozing water intake alarm for 20 minutes.");
            
            long snoozeTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(20);

            // Respect 11 PM limit
            Calendar elevenPM = Calendar.getInstance();
            elevenPM.set(Calendar.HOUR_OF_DAY, 23);
            elevenPM.set(Calendar.MINUTE, 0);
            elevenPM.set(Calendar.SECOND, 0);
            elevenPM.set(Calendar.MILLISECOND, 0);

            if (snoozeTime > elevenPM.getTimeInMillis()) {
                Log.i(TAG, "Snooze time past 11 PM. Scheduling for tomorrow 11 AM.");
                snoozeTime = getNextDayStart();
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, WaterIntakeReceiver.class);
            intent.setAction(AppConstants.ACTION_WATER_ALARM);
            intent.putExtra("TASK_ID", def.getTaskId());
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    ALARM_REQUEST_CODE, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTime,
                        pendingIntent
                );
                Log.i(TAG, "Water alarm (SNOOZE) set for: " + new Date(snoozeTime));
                logOperation(context, "SNOOZE", "Water Alarm", "Snoozed until " + new Date(snoozeTime));

                // Update TaskState
                updateTaskState(context, def.getTaskId(), snoozeTime);
            }
        });
    }

    public static void logOperation(Context context, String status, String step, String message) {
        BackgroundJobLog log = new BackgroundJobLog(
                "WATER_" + DateUtils.convertTimestampintoHH_MM_SS_String(System.currentTimeMillis()),
                WATER_WORKER,
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
