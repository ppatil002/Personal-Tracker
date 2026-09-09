package com.example.pp.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.RegistryDao;
import com.example.pp.data.local.entity.RegistryEntity;

public class RegistryRepository {
    private final RegistryDao registryDao;
    private final AppExecutors executors;

    public RegistryRepository(Context context) {
        this.registryDao = AppDatabase.getInstance(context).registryDao();
        this.executors = AppExecutors.getInstance();
    }

    public void saveRegistryItem(String item, String value, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            RegistryEntity entity = new RegistryEntity(item, value, System.currentTimeMillis());
            registryDao.insertOrUpdate(entity);
            if (onComplete != null) {
                executors.mainThread().execute(onComplete);
            }
        });
    }

    public LiveData<RegistryEntity> getRegistryItemLive(String item) {
        return registryDao.getRegistryItemLive(item);
    }

    public void getRegistryItem(String item, RegistryCallback callback) {
        executors.diskIO().execute(() -> {
            RegistryEntity entity = registryDao.getRegistryItem(item);
            executors.mainThread().execute(() -> callback.onLoaded(entity));
        });
    }

    public interface RegistryCallback {
        void onLoaded(RegistryEntity entity);
    }
}
