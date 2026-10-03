package com.example.pp.core.orchestrator;

import android.content.Context;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.core.orchestrator.backup.BackupScheduler;
import com.example.pp.core.orchestrator.history.HistoryScheduler;
import com.example.pp.core.orchestrator.quotes.QuoteScheduler;
import com.example.pp.core.orchestrator.screentime.ScreenTimeScheduler;
import com.example.pp.core.orchestrator.sleep.SleepScheduler;
import com.example.pp.core.orchestrator.vocabulary.VocabularyScheduler;
import com.example.pp.core.orchestrator.water.WaterIntakeScheduler;
import com.example.pp.data.local.dao.TaskDao;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.repository.TaskRepository;

import java.util.List;

public class SystemTaskScheduler {
    private static final String TAG = "SystemTaskScheduler";

    public static void scheduleAllTasksSync(Context context) {
        Context appContext = context.getApplicationContext();
        TaskRepository taskRepository = ((PPApplication) appContext).getTaskRepository();
        List<TaskDao.TaskWithState> tasks = taskRepository.getEnabledTasksWithState();

        for (TaskDao.TaskWithState task : tasks) {
            TaskDefinition def = task.definition;
            routeToNativeScheduler(appContext, def);
        }
    }

    public static void pauseTask(Context context, TaskDefinition def) {
        Log.d(TAG, "Pausing task: " + def.getTaskType());
        switch (def.getTaskType()) {
            case WATER_REMINDER:
                WaterIntakeScheduler.cancel(context);
                break;
            case SLEEP_REMINDER:
                SleepScheduler.cancel(context);
                break;
            case SCREEN_TIME_SYNC:
                ScreenTimeScheduler.cancel(context);
                break;
            case BACKUP:
                BackupScheduler.cancel(context);
                break;
            case QUOTES:
                QuoteScheduler.cancel(context);
                break;
            case HISTORY:
                HistoryScheduler.cancel(context);
                break;
            case VOCABULARY_REVISION:
                VocabularyScheduler.cancel(context);
                break;
        }
    }

    public static void resumeTask(Context context, TaskDefinition def) {
        Log.d(TAG, "Resuming task: " + def.getTaskType());
        routeToNativeScheduler(context, def);
    }

    private static void routeToNativeScheduler(Context context, TaskDefinition def) {
        Log.d(TAG, "Configuring scheduling pipeline for task type: " + def.getTaskType());
        switch (def.getTaskType()) {
            case BACKUP:
                BackupScheduler.schedule(context, def);
                break;
            case WATER_REMINDER:
                WaterIntakeScheduler.schedule(context, def);
                break;
            case SCREEN_TIME_SYNC:
                ScreenTimeScheduler.schedule(context, def);
                break;
            case SLEEP_REMINDER:
                SleepScheduler.schedule(context, def);
                break;
            case QUOTES:
                QuoteScheduler.schedule(context, def);
                break;
            case HISTORY:
                HistoryScheduler.schedule(context, def);
                break;
            case VOCABULARY_REVISION:
                VocabularyScheduler.schedule(context, def);
                break;
            default:
                Log.w(TAG, "No native scheduler found for task type: " + def.getTaskType());
                break;
        }
    }
}
