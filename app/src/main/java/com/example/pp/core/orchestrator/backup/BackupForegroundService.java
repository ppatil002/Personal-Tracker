package com.example.pp.core.orchestrator.backup;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.pp.PPApplication;
import com.example.pp.core.enums.TaskStatus;
import com.example.pp.core.enums.TaskType;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;
import com.example.pp.data.repository.TaskRepository;
import com.example.pp.utils.ConstraintEvaluator;

public class BackupForegroundService extends Service {
    private static final String TAG = "BackupService";

    private TaskRepository repository;
    private AppExecutors executors;
    private volatile boolean isRunning = false;

    private final BroadcastReceiver constraintReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkAndStartBackup();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        repository = ((PPApplication) getApplication()).getTaskRepository();
        executors = AppExecutors.getInstance();
        BackupNotificationHelper.createNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting Backup Foreground Service");

        startForeground(BackupNotificationHelper.NOTIFICATION_ID, 
                BackupNotificationHelper.createNotification(this, "Initializing...", true),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(constraintReceiver, filter);

        checkAndStartBackup();

        return START_STICKY;
    }

    private void checkAndStartBackup() {
        if (isRunning) return;

        executors.diskIO().execute(() -> {
            if (isRunning) return;

            TaskDefinition def = repository.getDefinitionByType(TaskType.BACKUP);
            if (def == null) {
                executors.mainThread().execute(this::stopSelf);
                return;
            }

            boolean internet = ConstraintEvaluator.isInternetAvailable(this);
            int battery = ConstraintEvaluator.getBatteryLevel(this);
            boolean batteryOk = battery >= def.getMinimumBattery();

            if (internet && batteryOk) {
                isRunning = true;
                executors.mainThread().execute(() -> {
                    unregisterReceiverSafe();
                    executeBackupChain(def);
                });
            } else {
                StringBuilder reason = new StringBuilder();
                if (!internet) reason.append("Waiting for Wi-Fi/Internet... ");
                if (!batteryOk) reason.append("Waiting for Charger (Battery < ").append(def.getMinimumBattery()).append("%).");
                BackupNotificationHelper.updateNotification(this, reason.toString());
            }
        });
    }

    private void executeBackupChain(TaskDefinition def) {
        Log.i(TAG, "All constraints met. Starting backup chain logic...");

        executors.diskIO().execute(() -> {
            TaskState state = repository.getStateByType(TaskType.BACKUP);
            if (state != null) {
                state.setCurrentState(TaskStatus.RUNNING);
                state.setRunning(true);
                state.setProgress(10);
                state.setLastRunTime(System.currentTimeMillis());
                repository.updateState(state);
            }

            BackupNotificationHelper.updateNotification(this, "Backup in progress: 10%");

            String uniqueName = "BACKUP_CHAIN_" + def.getTaskId();
            
            executors.mainThread().execute(() -> {
                BackupManager.getInstance(this).runBackupChain(def.getTaskId());

                androidx.work.WorkManager.getInstance(this)
                        .getWorkInfosForUniqueWorkLiveData(uniqueName)
                        .observeForever(workInfos -> {
                            if (workInfos != null && !workInfos.isEmpty()) {
                                androidx.work.WorkInfo latest = workInfos.get(workInfos.size() - 1);
                                androidx.work.WorkInfo.State s = latest.getState();

                                if (s == androidx.work.WorkInfo.State.SUCCEEDED) {
                                    BackupNotificationHelper.showFinalNotification(this, "Backup Complete!");
                                    stopForeground(STOP_FOREGROUND_DETACH);
                                    stopSelf();
                                } else if (s == androidx.work.WorkInfo.State.FAILED) {
                                    String error = latest.getOutputData().getString("ERROR_DETAIL");
                                    BackupNotificationHelper.showFinalNotification(this, 
                                            error != null ? "Backup Failed: " + error : "Backup Failed");
                                    stopForeground(STOP_FOREGROUND_DETACH);
                                    stopSelf();
                                } else {
                                    BackupNotificationHelper.updateNotification(this, "Processing backup...");
                                }
                            }
                        });
            });
        });
    }

    private void unregisterReceiverSafe() {
        try {
            unregisterReceiver(constraintReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    public void onDestroy() {
        unregisterReceiverSafe();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
