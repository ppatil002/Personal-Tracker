package com.example.pp;

import android.app.Application;

import com.example.pp.data.repository.CategoryRepository;
import com.example.pp.data.repository.GenericTableRepository;
import com.example.pp.data.repository.TagRepository;
import com.example.pp.data.repository.TransactionRepository;
import com.example.pp.infra.background.MainOrchestratorWorker;
import com.jakewharton.threetenabp.AndroidThreeTen;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class PPApplication extends Application {
    private CategoryRepository categoryRepository;
    private TagRepository tagRepository;
    private TransactionRepository transactionRepository;
    private GenericTableRepository genericTableRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);
        categoryRepository = new CategoryRepository(this);
        tagRepository = new TagRepository(this);
        transactionRepository = new TransactionRepository(this);
        genericTableRepository = new GenericTableRepository(this);
        scheduleSync();
    }

    private void scheduleSync() {
        // Scheduled every 24 hours, but execution depends on OS constraints (charging + wifi)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresCharging(true)
                .build();

        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                MainOrchestratorWorker.class, 24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "PP_SYNC_WORK",
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
                syncRequest
        );
    }

    public CategoryRepository getCategoryRepository() {
        return categoryRepository;
    }

    public TagRepository getTagRepository() {
        return tagRepository;
    }

    public TransactionRepository getTransactionRepository() {
        return transactionRepository;
    }

    public GenericTableRepository getGenericTableRepository() {
        return genericTableRepository;
    }
}