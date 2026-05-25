package com.example.pp.core.orchestrator.backup;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.pp.PPApplication;
import com.example.pp.core.enums.TaskStatus;
import com.example.pp.core.enums.TaskType;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.utils.ConstraintEvaluator;

public class BackupWorker extends Worker {
    private static final String TAG = "BackupWorker";

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        TaskRepository repository = ((PPApplication) getApplicationContext()).getTaskRepository();
        
        TaskDefinition def = repository.getDefinitionByType(TaskType.BACKUP);
        TaskState state = repository.getStateByType(TaskType.BACKUP);

        if (def == null || state == null) {
            Log.e(TAG, "Backup task definition or state not found.");
            return Result.failure();
        }

        // Manual constraint check to trigger notification if system constraints aren't enough
        // or if we want to inform the user why it's not running right now.
        if (!ConstraintEvaluator.checkConstraints(context, def)) {
            Log.w(TAG, "Constraints not met for backup. Showing notification.");
            BackupNotificationHelper.showConstraintFailureNotification(context, def, state);
            return Result.retry(); // Retry later when conditions might change
        }

        Log.i(TAG, "Constraints met. Initiating backup chain.");
        BackupNotificationHelper.cancelNotification(context);

        // Update task state in database to reflect that it is running
        state.setCurrentState(TaskStatus.RUNNING);
        state.setRunning(true);
        state.setProgress(0);
        state.setLastFailureReason(null);
        repository.updateState(state);

        BackupManager.getInstance(context).runBackupChain(def.getTaskId());

        return Result.success();
    }
}
