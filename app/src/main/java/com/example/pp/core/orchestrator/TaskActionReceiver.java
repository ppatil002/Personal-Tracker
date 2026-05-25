package com.example.pp.core.orchestrator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pp.PPApplication;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.core.orchestrator.backup.BackupNotificationHelper;

public class TaskActionReceiver extends BroadcastReceiver {
    private static final String TAG = "TaskActionReceiver";
    public static final String ACTION_RESUME = "com.example.pp.ACTION_RESUME_TASK";
    public static final String ACTION_SNOOZE = "com.example.pp.ACTION_SNOOZE_TASK";
    public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String taskId = intent.getStringExtra(EXTRA_TASK_ID);

        if (taskId == null) return;

        TaskRepository taskrepository = ((PPApplication) context.getApplicationContext()).getTaskRepository();
        

        if (ACTION_RESUME.equals(action)) {
            Log.i(TAG, "!!! RESUME ACTION RECEIVED !!! for task: " + taskId);
            taskrepository.runTaskNow(taskId);
            BackupNotificationHelper.cancelNotification(context);
        } else if (ACTION_SNOOZE.equals(action)) {
            Log.i(TAG, "!!! SNOOZE ACTION RECEIVED !!! for task: " + taskId);
            taskrepository.snoozeTask(taskId);
            BackupNotificationHelper.cancelNotification(context);
        }
    }
}
