package com.example.pp.data.repository;

import android.content.Context;

import com.example.pp.core.cache.NutrientCacheManager;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.NutrientDao;
import com.example.pp.data.local.entity.NutrientEntity;

import java.util.List;

public class NutrientRepository {
    private final NutrientDao nutrientDao;
    private final NutrientCacheManager cacheManager;
    private final AppExecutors executors;

    public NutrientRepository(Context context) {
        this.nutrientDao = AppDatabase.getInstance(context).nutrientDao();
        this.cacheManager = NutrientCacheManager.getInstance();
        this.executors = AppExecutors.getInstance();
    }

    /**
     * Get all nutrients. Uses cache if available and not dirty.
     */
    public void getAllNutrients(OnNutrientsLoadedCallback callback) {
        executors.diskIO().execute(() -> {
            if (cacheManager.isDirty()) {
                List<NutrientEntity> nutrients = nutrientDao.getAll();
                cacheManager.updateCache(nutrients);
            }
            List<NutrientEntity> result = cacheManager.getCachedNutrients();
            executors.mainThread().execute(() -> callback.onLoaded(result));
        });
    }

    public void addNutrient(NutrientEntity nutrient, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            nutrientDao.insert(nutrient);
            cacheManager.setDirty(true);
            if (onComplete != null) executors.mainThread().execute(onComplete);
        });
    }

    public void updateNutrient(NutrientEntity nutrient, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            nutrientDao.update(nutrient);
            cacheManager.setDirty(true);
            if (onComplete != null) executors.mainThread().execute(onComplete);
        });
    }

    public void deleteNutrient(NutrientEntity nutrient, Runnable onComplete) {
        executors.diskIO().execute(() -> {
            nutrientDao.delete(nutrient);
            cacheManager.setDirty(true);
            if (onComplete != null) executors.mainThread().execute(onComplete);
        });
    }

    public void seedNutrientData() {
        executors.diskIO().execute(() -> {
            if (nutrientDao.getCount() == 0) {
                long now = System.currentTimeMillis();
                nutrientDao.insert(new NutrientEntity("Milk", 42, 3.4, 5, 1, 100, "ml", now, now));
                nutrientDao.insert(new NutrientEntity("Egg", 155, 13, 1.1, 11, 100, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Chicken Breast", 165, 31, 0, 3.6, 100, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Rice", 130, 2.7, 28, 0.3, 100, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Banana", 89, 1.1, 23, 0.3, 100, "gram", now, now));
                cacheManager.setDirty(true);
            }
        });
    }

    public interface OnNutrientsLoadedCallback {
        void onLoaded(List<NutrientEntity> nutrients);
    }
}
