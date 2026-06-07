package com.example.pp.core.cache;

import com.example.pp.data.local.entity.NutrientEntity;

import java.util.ArrayList;
import java.util.List;

public class NutrientCacheManager {
    private static volatile NutrientCacheManager instance;
    private List<NutrientEntity> cachedNutrients;
    private boolean isDirty = true;

    private NutrientCacheManager() {
        this.cachedNutrients = new ArrayList<>();
    }

    public static NutrientCacheManager getInstance() {
        if (instance == null) {
            synchronized (NutrientCacheManager.class) {
                if (instance == null) {
                    instance = new NutrientCacheManager();
                }
            }
        }
        return instance;
    }

    public List<NutrientEntity> getCachedNutrients() {
        return cachedNutrients;
    }

    public void updateCache(List<NutrientEntity> nutrients) {
        this.cachedNutrients = nutrients;
        this.isDirty = false;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }
}
