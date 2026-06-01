package com.example.pp.features.backup;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.pp.PPApplication;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.data.repository.BackgroundJobLogRepository;
import com.example.pp.features.backup.worker.CloudDownloadWorker;
import com.example.pp.features.backup.worker.CryptoDecryptWorker;
import com.example.pp.features.backup.worker.DataImportWorker;

import java.util.List;
import java.util.UUID;

public class BackupViewModel extends AndroidViewModel {

    private final LiveData<List<BackgroundJobLog>> logs;
    private final WorkManager workManager;

    public BackupViewModel(@NonNull Application application) {
        super(application);
        BackgroundJobLogRepository backgroundJobLogRepository = ((PPApplication) application).getBackgroundJobLogRepository();
        logs = backgroundJobLogRepository.getAllLogs();
        workManager = WorkManager.getInstance(application);
    }

    public LiveData<List<BackgroundJobLog>> getLogs() {
        return logs;
    }

    public void runManualBackup() {
        Log.i("BackupViewModel", "Manual backup requested by user.");
        // Use the TaskRepository to trigger the task immediately, bypassing normal scheduling/constraints
        PPApplication app = (PPApplication) getApplication();
        app.getTaskRepository().runTaskNow("BACKUP_TASK_001");
    }

    public UUID runRestore(String remoteFileName) {
        androidx.work.Data data = new androidx.work.Data.Builder()
                .putString("REMOTE_FILE_NAME", remoteFileName)
                .build();

        OneTimeWorkRequest downloadTask = new OneTimeWorkRequest.Builder(CloudDownloadWorker.class)
                .setInputData(data).build();
        OneTimeWorkRequest decryptTask = new OneTimeWorkRequest.Builder(CryptoDecryptWorker.class).build();
        OneTimeWorkRequest importTask = new OneTimeWorkRequest.Builder(DataImportWorker.class).build();

        workManager.beginWith(downloadTask)
                   .then(decryptTask)
                   .then(importTask)
                   .enqueue();

        return importTask.getId();
    }
    
    public WorkManager getWorkManager() {
        return workManager;
    }
}
