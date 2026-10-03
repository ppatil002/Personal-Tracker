package com.example.pp.data.repository;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.pp.core.enums.TaskStatus;
import com.example.pp.core.enums.TaskType;
import com.example.pp.core.orchestrator.SystemTaskScheduler;
import com.example.pp.core.orchestrator.backup.BackupForegroundService;
import com.example.pp.core.orchestrator.history.HistoryWorker;
import com.example.pp.core.orchestrator.quotes.QuoteWorker;
import com.example.pp.core.orchestrator.screentime.ScreenTimeScheduler;
import com.example.pp.core.orchestrator.vocabulary.VocabularyScheduler;
import com.example.pp.core.orchestrator.water.WaterNotificationHelper;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.TaskDao;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private final TaskDao taskDao;
    private final AppExecutors executors;
    private final Context context;

    public TaskRepository(Context context) {
        this.context = context.getApplicationContext();
        this.taskDao = AppDatabase.getInstance(context).taskDao();
        this.executors = AppExecutors.getInstance();
    }

    public void seedTasksIfEmpty() {
        executors.diskIO().execute(() -> {
            try {
                Log.d(TAG, "Checking for existing tasks in DB...");
                List<TaskDao.TaskWithState> existing = taskDao.getAllTasksWithState();
                
                // CRITICAL: Cleanup any tasks that were left in "Running" state due to app crash/kill
                for (TaskDao.TaskWithState task : existing) {
                    if (task.state.isRunning()) {
                        Log.w(TAG, "Found stale running task: " + task.definition.getTaskType() + ". Resetting state.");
                        task.state.setRunning(false);
                        task.state.setCurrentState(TaskStatus.SCHEDULED);
                        taskDao.updateState(task.state);
                    }
                }

                seedDefaultTasks();
                
                // Schedule the periodic tasks via the new SystemTaskScheduler
                SystemTaskScheduler.scheduleAllTasksSync(context);
                
            } catch (Exception e) {
                Log.e(TAG, "CRITICAL ERROR while seeding tasks", e);
            }
        });
    }

    private void seedDefaultTasks() {
        seedBackupTask();
        seedWaterTask();
        seedScreenTimeTask();
        seedSleepTask();
        seedQuotesTask();
        seedHistoryTask();
        seedVocabularyRevision();
    }

    private void seedVocabularyRevision() {
        TaskDefinition vocabDef = new TaskDefinition(
                "VOCAB_REVISION_TASK_001",
                TaskType.VOCABULARY_REVISION,
                true,
                TimeUnit.HOURS.toMillis(4),
                1,
                false,
                0,
                false,
                30,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        TaskState vocabState = new TaskState(
                "VOCAB_REVISION_TASK_001",
                TaskStatus.SCHEDULED,
                0,
                0,
                0,
                0,
                0,
                null,
                false,
                false,
                0
        );

        taskDao.insertDefinition(vocabDef);
        taskDao.insertState(vocabState);
    }

    private void seedQuotesTask() {
        TaskDefinition quotesDef = new TaskDefinition(
                "QUOTES_TASK_001",
                TaskType.QUOTES,
                true,
                TimeUnit.HOURS.toMillis(24),
                1,
                true,
                0,
                false,
                60,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        TaskState quotesState = new TaskState(
                "QUOTES_TASK_001",
                TaskStatus.SCHEDULED,
                0,
                0,
                0,
                0,
                0,
                null,
                false,
                false,
                0
        );

        taskDao.insertDefinition(quotesDef);
        taskDao.insertState(quotesState);
    }

    private void seedHistoryTask() {
        TaskDefinition historyDef = new TaskDefinition(
                "HISTORY_TASK_001",
                TaskType.HISTORY,
                true,
                TimeUnit.HOURS.toMillis(24),
                1,
                true,
                0,
                false,
                60,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        TaskState historyState = new TaskState(
                "HISTORY_TASK_001",
                TaskStatus.SCHEDULED,
                0,
                0,
                0,
                0,
                0,
                null,
                false,
                false,
                0
        );

        taskDao.insertDefinition(historyDef);
        taskDao.insertState(historyState);
    }

    private void seedSleepTask() {
        TaskDefinition sleepDef = new TaskDefinition(
                "SLEEP_TASK_001",
                TaskType.SLEEP_REMINDER,
                true,
                0, // Dynamic interval handled by SleepScheduler
                1,
                false,
                0,
                false,
                30,
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        TaskState sleepState = new TaskState(
                "SLEEP_TASK_001",
                TaskStatus.SCHEDULED,
                0,
                0,
                0,
                0,
                0,
                null,
                false,
                false,
                0
        );

        taskDao.insertDefinition(sleepDef);
        taskDao.insertState(sleepState);
    }

    private void seedBackupTask() {
        TaskDefinition backupDef = new TaskDefinition(
                "BACKUP_TASK_001",
                TaskType.BACKUP,
                true,
                TimeUnit.HOURS.toMillis(24), // 24-hour interval
                1,
                true,
                20, // Minimum Battery: 20%
                false,
                120, // Snooze for 2 hours (120 minutes)
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        TaskState backupState = new TaskState(
                "BACKUP_TASK_001",
                TaskStatus.SCHEDULED,
                0,
                0,
                0,
                0,
                0,
                null,
                false,
                false,
                0
        );

        taskDao.insertDefinition(backupDef);
        taskDao.insertState(backupState);
    }

    private void seedWaterTask() {
        TaskDefinition waterDef = new TaskDefinition(
                "WATER_TASK_001",
                TaskType.WATER_REMINDER,
                true,
                TimeUnit.HOURS.toMillis(2), // 2-hour interval
                1,
                false,
                0, // Minimum Battery: 0%
                false,
                20, // Snooze for 20 minutes
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        TaskState waterState = new TaskState(
                "WATER_TASK_001",
                TaskStatus.SCHEDULED,
                0,
                0,
                0,
                0,
                0,
                null,
                false,
                false,
                0
        );

        taskDao.insertDefinition(waterDef);
        taskDao.insertState(waterState);
    }

    private void seedScreenTimeTask() {
        TaskDefinition screenTimeDef = new TaskDefinition(
                "SCREENTIME_TASK_001",
                TaskType.SCREEN_TIME_SYNC,
                true,
                TimeUnit.MINUTES.toMillis(15), // 15-minute interval
                1,
                false,
                0, // Minimum Battery: 0%
                false,
                5, // Snooze for 5 minutes
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        TaskState screenTimeState = new TaskState(
                "SCREENTIME_TASK_001",
                TaskStatus.SCHEDULED,
                0,
                0,
                0,
                0,
                0,
                null,
                false,
                false,
                0
        );

        taskDao.insertDefinition(screenTimeDef);
        taskDao.insertState(screenTimeState);
    }

    public List<TaskDao.TaskWithState> getEnabledTasksWithState() {
        return taskDao.getEnabledTasksWithState();
    }

    public List<TaskDao.TaskWithState> getAllTasksWithState() {
        return taskDao.getAllTasksWithState();
    }

    public androidx.lifecycle.LiveData<List<TaskDao.TaskWithState>> getAllTasksWithStateLiveData() {
        return taskDao.getAllTasksWithStateLiveData();
    }

    public void updateState(TaskState state) {
        taskDao.updateState(state);
    }

    public TaskState getStateForTask(String taskId) {
        return taskDao.getStateForTask(taskId);
    }

    public TaskDefinition getDefinitionByType(TaskType type) {
        return taskDao.getDefinitionByType(type);
    }

    public TaskState getStateByType(TaskType type) {
        return taskDao.getStateByType(type);
    }

    public void runTaskNow(String taskId) {
        executors.diskIO().execute(() -> {
            TaskDefinition def = taskDao.getDefinitionById(taskId);
            if (def != null) {
                Log.i(TAG, "Triggering task manually: " + taskId);

                // Update state to RUNNING to avoid Orchestrator double-triggering
                TaskState state = taskDao.getStateForTask(taskId);
                if (state != null) {
                    state.setCurrentState(TaskStatus.RUNNING);
                    state.setRunning(true);
                    state.setAwaitingUserAction(false);
                    taskDao.updateState(state);
                }
                
                if (def.getTaskType() == TaskType.BACKUP) {
                    Intent serviceIntent = new Intent(context, BackupForegroundService.class);
                    serviceIntent.putExtra("TASK_ID", taskId);
                    context.startForegroundService(serviceIntent);
                } else if (def.getTaskType() == TaskType.WATER_REMINDER) {
                    WaterNotificationHelper.showNotification(context, taskId);
                } else if (def.getTaskType() == TaskType.SCREEN_TIME_SYNC) {
                    ScreenTimeScheduler.runNow(context, taskId);
                } else if (def.getTaskType() == TaskType.QUOTES) {
                    OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(QuoteWorker.class)
                            .setInputData(new Data.Builder().putString("TASK_ID", taskId).build())
                            .build();
                    WorkManager.getInstance(context).enqueue(work);
                } else if (def.getTaskType() == TaskType.HISTORY) {
                    OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(HistoryWorker.class)
                            .setInputData(new Data.Builder().putString("TASK_ID", taskId).build())
                            .build();
                    WorkManager.getInstance(context).enqueue(work);
                } else if (def.getTaskType() == TaskType.VOCABULARY_REVISION) {
                    VocabularyScheduler.triggerNow(context, taskId);
                }
            }
        });
    }

    public void pauseTask(String taskId) {
        executors.diskIO().execute(() -> {
            TaskDefinition def = taskDao.getDefinitionById(taskId);
            if (def != null) {
                def.setEnabled(false);
                taskDao.updateDefinition(def);

                TaskState state = taskDao.getStateForTask(taskId);
                if (state != null) {
                    state.setCurrentState(TaskStatus.PAUSED);
                    state.setNextRunTime(0);
                    taskDao.updateState(state);
                }

                SystemTaskScheduler.pauseTask(context, def);
            }
        });
    }

    public void resumeTask(String taskId) {
        executors.diskIO().execute(() -> {
            TaskDefinition def = taskDao.getDefinitionById(taskId);
            if (def != null) {
                def.setEnabled(true);
                taskDao.updateDefinition(def);

                TaskState state = taskDao.getStateForTask(taskId);
                if (state != null) {
                    state.setCurrentState(TaskStatus.IDLE);
                    taskDao.updateState(state);
                }

                SystemTaskScheduler.resumeTask(context, def);
            }
        });
    }

    public void snoozeTask(String taskId) {
        executors.diskIO().execute(() -> {
            TaskDefinition def = taskDao.getDefinitionById(taskId);
            TaskState state = taskDao.getStateForTask(taskId);
            if (def != null && state != null) {
                long snoozeUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(def.getSnoozeIntervalMinutes());
                state.setCurrentState(TaskStatus.SNOOZED);
                state.setSnoozedUntil(snoozeUntil);
                state.setNextRunTime(snoozeUntil); // Also update next run time
                state.setAwaitingUserAction(false);
                taskDao.updateState(state);
                Log.i(TAG, "Task " + taskId + " snoozed until " + snoozeUntil);
            }
        });
    }

    public WorkManager getWorkManager() {
        return WorkManager.getInstance(context);
    }
}
