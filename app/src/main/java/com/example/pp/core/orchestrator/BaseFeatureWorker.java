package com.example.pp.core.orchestrator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.core.constants.AppConstants;
import com.example.pp.core.enums.TaskStatus;
import com.example.pp.core.enums.TaskType;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.TaskDao;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.repository.BackgroundJobLogRepository;

public abstract class BaseFeatureWorker extends Worker {

    protected final TaskDao taskDao;
    protected final String jobId;
    protected final String taskId;

    private static final String CHANNEL_ID = AppConstants.CHANNEL_TASK_ENGINE;
    private static final int NOTIFICATION_ID = AppConstants.NOTIFY_ID_TASK_ENGINE;

    public BaseFeatureWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        AppDatabase db = AppDatabase.getInstance(context);
        this.taskDao = db.taskDao();
        this.jobId = params.getInputData().getString("EXECUTION_SESSION_ID") != null ? 
                     params.getInputData().getString("EXECUTION_SESSION_ID") : getId().toString();
        this.taskId = params.getInputData().getString("TASK_ID");
    }

    protected void updateLog(String status, String step, String message) {
        BackgroundJobLog log = new BackgroundJobLog(
                jobId,
                getClass().getSimpleName(),
                status,
                step,
                message,
                System.currentTimeMillis()
        );
        BackgroundJobLogRepository backgroundJobLogRepository = ((PPApplication) getApplicationContext())
                .getBackgroundJobLogRepository();
        backgroundJobLogRepository.insert(log);
    }

    protected void updateTaskProgress(int progress) {
        if (taskId != null) {
            TaskState state = taskDao.getStateForTask(taskId);
            if (state != null) {
                state.setProgress(progress);
                taskDao.updateState(state);
            }
        }
    }

    protected void completeTask() {
        if (taskId != null) {
            TaskState state = taskDao.getStateForTask(taskId);
            TaskDefinition def = taskDao.getDefinitionById(taskId);
            if (state != null && def != null) {
                long now = System.currentTimeMillis();
                state.setCurrentState(TaskStatus.COMPLETED);
                state.setRunning(false);
                state.setLastRunTime(now);
                
                // For Backup, set next run to tomorrow at 12 PM (as per scheduler logic)
                // For others, use repeat interval.
                if (def.getTaskType() == TaskType.BACKUP) {
                    java.util.Calendar next = java.util.Calendar.getInstance();
                    next.set(java.util.Calendar.HOUR_OF_DAY, 12);
                    next.set(java.util.Calendar.MINUTE, 0);
                    next.set(java.util.Calendar.SECOND, 0);
                    if (next.getTimeInMillis() <= now) {
                        next.add(java.util.Calendar.DAY_OF_YEAR, 1);
                    }
                    state.setNextRunTime(next.getTimeInMillis());
                } else {
                    state.setNextRunTime(now + def.getRepeatInterval());
                }

                state.setProgress(100);
                state.setLastFailureReason(null);
                taskDao.updateState(state);
            }
        }
    }

    protected void failTask(String reason) {
        if (taskId != null) {
            TaskState state = taskDao.getStateForTask(taskId);
            if (state != null) {
                state.setCurrentState(TaskStatus.FAILED);
                state.setRunning(false);
                state.setLastFailureReason(reason);
                taskDao.updateState(state);
            }
        }
    }

    protected ForegroundInfo createForegroundInfo(String taskName, String currentStep, int statusStyle) {
        Context ctx = getApplicationContext();
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cloud_upload)
                .setContentTitle("Task Engine: " + taskName)
                .setContentText("[" + currentStep + "] in progress...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setColor(statusStyle == 1 ? Color.parseColor("#22D3EE") : Color.parseColor("#B3B3C3"))
                .setOngoing(statusStyle == 1)
                .build();

        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Background Tasks",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }
}
