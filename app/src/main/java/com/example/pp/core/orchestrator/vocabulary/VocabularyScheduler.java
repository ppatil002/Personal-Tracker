package com.example.pp.core.orchestrator.vocabulary;

import static com.example.pp.core.constants.AppConstants.VOCABULARY_WORKER;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.TaskStatus;
import com.example.pp.core.enums.TaskType;
import com.example.pp.core.ml.HalfLifeEngine;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.local.entity.Vocabulary;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.utils.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class VocabularyScheduler {
    private static final String TAG = "VocabularyScheduler";
    public static final int ALARM_REQUEST_CODE = AppConstants.RC_VOCABULARY_ALARM;

    // Hardcoded reminder schedule times: 12:30, 15:30 (3:30 PM), 17:30 (5:30 PM), 19:00 (7 PM), 23:30 (11:30 PM)
    private static final int[][] SCHEDULE_TIMES = {
            {12, 30},
            {15, 30},
            {17, 30},
            {19, 0},
            {23, 30}
    };

    /**
     * Schedules the next vocabulary revision alarm based on the database state.
     */
    public static void schedule(Context context, TaskDefinition def) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            TaskDefinition taskDef = def;
            if (taskDef == null) {
                TaskRepository taskRepository = ((PPApplication) context.getApplicationContext()).getTaskRepository();
                taskDef = taskRepository.getDefinitionByType(TaskType.VOCABULARY_REVISION);
            }

            if (taskDef == null || !taskDef.isEnabled()) {
                Log.i(TAG, "Vocabulary revision is disabled. Skipping schedule.");
                return;
            }

            long nextAlarmTime = calculateNextTriggerTime(System.currentTimeMillis());

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, VocabularyReceiver.class);
            intent.setAction(AppConstants.ACTION_VOCABULARY_ALARM);
            intent.putExtra("TASK_ID", taskDef.getTaskId());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
//                allowed to execute while the phone is idling in Doze mode  it is an inexact method, Android will group it with other app syncs to save battery
//                TODO If performance issue then change it to setExactAndAllowWhileIdle
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextAlarmTime,
                        pendingIntent
                );
                Log.i(TAG, "Vocabulary alarm set for: " + new Date(nextAlarmTime));
                logOperation(context, "SCHEDULE", "Vocab Revise", "Alarm set for " + new Date(nextAlarmTime));

                // Update TaskState
                updateTaskState(context, taskDef.getTaskId(), nextAlarmTime);
            }
        });
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, VocabularyReceiver.class);
        intent.setAction(AppConstants.ACTION_VOCABULARY_ALARM);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.i(TAG, "Vocabulary revision alarm cancelled.");
            logOperation(context, "CANCEL", "Vocab Alarm", "Vocabulary alarm cancelled manually.");
        }
    }

    public static void triggerNow(Context context, String taskId) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            Vocabulary word = getNextWordToRevise(context);
            if (word != null) {
                VocabularyNotificationHelper.showInitialNotification(context, word, taskId);
            } else {
                Log.w(TAG, "No vocabulary available to trigger notification.");
            }
        });
    }

    /**
     * Calculates the next reminder trigger time based on fixed schedule times:
     * 12:30, 15:30 (3:30 PM), 17:30 (5:30 PM), 19:00 (7 PM), 23:30 (11:30 PM).
     * Hardcoded for now; can be modified later with dynamic scheduling logic.
     */
    public static long calculateNextTriggerTime(long currentMillis) {
        Calendar now = Calendar.getInstance();
        now.setTimeInMillis(currentMillis);

        for (int[] time : SCHEDULE_TIMES) {
            Calendar target = (Calendar) now.clone();
            target.set(Calendar.HOUR_OF_DAY, time[0]);
            target.set(Calendar.MINUTE, time[1]);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);

            if (target.getTimeInMillis() > currentMillis + 1000) {
                return target.getTimeInMillis();
            }
        }

        // If all times for today have passed, schedule for 12:30 tomorrow
        Calendar tomorrowFirst = (Calendar) now.clone();
        tomorrowFirst.add(Calendar.DAY_OF_YEAR, 1);
        tomorrowFirst.set(Calendar.HOUR_OF_DAY, SCHEDULE_TIMES[0][0]);
        tomorrowFirst.set(Calendar.MINUTE, SCHEDULE_TIMES[0][1]);
        tomorrowFirst.set(Calendar.SECOND, 0);
        tomorrowFirst.set(Calendar.MILLISECOND, 0);

        return tomorrowFirst.getTimeInMillis();
    }

    /**
     * Uses HalfLifeEngine to fetch the highest priority overdue vocabulary item for revision.
     */
    public static Vocabulary getNextWordToRevise(Context context) {
        List<Vocabulary> dueList = HalfLifeEngine.getInstance(context).getDueWordsForNotification(1);
        if (dueList != null && !dueList.isEmpty()) {
            return dueList.get(0);
        }
        return null;
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

    public static void logOperation(Context context, String status, String step, String message) {
        BackgroundJobLog log = new BackgroundJobLog(
                "VOCAB_" + DateUtils.convertTimestampintoHH_MM_SS_String(System.currentTimeMillis()),
                VOCABULARY_WORKER,
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
