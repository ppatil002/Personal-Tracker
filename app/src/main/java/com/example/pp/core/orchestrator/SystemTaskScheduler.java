package com.example.pp.core.orchestrator;

import android.content.Context;
import android.util.Log;
import com.example.pp.PPApplication;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.dao.TaskDao;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.core.orchestrator.backup.BackupScheduler;
import java.util.List;

public class SystemTaskScheduler {
    private static final String TAG = "SystemTaskScheduler";

    public static void scheduleAllTasks(Context context) {
        Log.i(TAG, "Initializing native background scheduling registration...");
        AppExecutors.getInstance().diskIO().execute(() -> scheduleAllTasksSync(context));
    }

    public static void scheduleAllTasksSync(Context context) {
        Context appContext = context.getApplicationContext();
        TaskRepository taskRepository = ((PPApplication) appContext).getTaskRepository();
        List<TaskDao.TaskWithState> tasks = taskRepository.getEnabledTasksWithState();

        for (TaskDao.TaskWithState task : tasks) {
            TaskDefinition def = task.definition;
            routeToNativeScheduler(appContext, def);
        }
    }

    private static void routeToNativeScheduler(Context context, TaskDefinition def) {
        Log.d(TAG, "Configuring scheduling pipeline for task type: " + def.getTaskType());
        switch (def.getTaskType()) {
            case BACKUP:
                BackupScheduler.schedule(context, def);
                break;
            default:
                Log.w(TAG, "No native scheduler found for task type: " + def.getTaskType());
                break;
        }
    }
}
