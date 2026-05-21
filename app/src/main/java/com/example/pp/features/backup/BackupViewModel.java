package com.example.pp.features.backup;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.entity.BackgroundJobLog;
import com.example.pp.features.backup.worker.CloudDownloadWorker;
import com.example.pp.features.backup.worker.CryptoDecryptWorker;
import com.example.pp.features.backup.worker.DataImportWorker;
import com.example.pp.infra.background.MainOrchestratorWorker;

import java.util.List;
import java.util.UUID;

public class BackupViewModel extends AndroidViewModel {

    private final LiveData<List<BackgroundJobLog>> logs;
    private final WorkManager workManager;

    public BackupViewModel(@NonNull Application application) {
        super(application);
        logs = AppDatabase.getInstance(application).backgroundJobLogDao().getAllLogs();
        workManager = WorkManager.getInstance(application);
    }

    public LiveData<List<BackgroundJobLog>> getLogs() {
        return logs;
    }

    public void runManualBackup() {
        OneTimeWorkRequest backupRequest = new OneTimeWorkRequest.Builder(MainOrchestratorWorker.class).build();
        workManager.enqueue(backupRequest);
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
