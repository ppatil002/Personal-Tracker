package com.example.pp.core.orchestrator.backup;

import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.pp.features.backup.worker.CloudUploadWorker;
import com.example.pp.features.backup.worker.CryptoStreamWorker;
import com.example.pp.features.backup.worker.DataExportWorker;

public class BackupManager {
    private static BackupManager instance;
    private final Context context;

    private BackupManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized BackupManager getInstance(Context context) {
        if (instance == null) {
            instance = new BackupManager(context);
        }
        return instance;
    }

    public void runBackupChain(String taskId) {
        Log.i("BackupManager", "!!! INITIATING BACKUP CHAIN !!! Task ID: " + taskId);
        WorkManager workManager = WorkManager.getInstance(context);

        Data data = new Data.Builder()
                .putString("TASK_ID", taskId)
                .build();

        Log.d("BackupManager", "Step 1: DataExportWorker enqueued.");
        OneTimeWorkRequest exportTask = new OneTimeWorkRequest.Builder(DataExportWorker.class)
                .setInputData(data)
                .build();
        
        Log.d("BackupManager", "Step 2: CryptoStreamWorker enqueued.");
        OneTimeWorkRequest cryptoTask = new OneTimeWorkRequest.Builder(CryptoStreamWorker.class)
                .setInputData(data)
                .build();
        
        Log.d("BackupManager", "Step 3: CloudUploadWorker enqueued.");
        OneTimeWorkRequest uploadTask = new OneTimeWorkRequest.Builder(CloudUploadWorker.class)
                .setInputData(data)
                .build();

        workManager.beginUniqueWork("BACKUP_CHAIN_" + taskId, ExistingWorkPolicy.REPLACE, exportTask)
                .then(cryptoTask)
                .then(uploadTask)
                .enqueue();
        
        Log.i("BackupManager", "Backup WorkManager pipeline successfully enqueued.");
    }
}
