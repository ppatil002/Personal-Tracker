package com.example.pp.data.repository;

import android.content.Context;

import com.example.pp.core.cache.NutrientCacheManager;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.NutrientEntityDao;
import com.example.pp.data.local.entity.NutrientEntity;

import java.util.List;

public class NutrientRepository {
    private final NutrientEntityDao nutrientDao;
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

                // --- Table 1: Poultry Eggs, Milk, and Fermented Dairy Curd ---
                nutrientDao.insert(new NutrientEntity("Egg", 85.0, 6.60, 0.0, 6.50, 1, "unit", now, now));
                nutrientDao.insert(new NutrientEntity("Egg White", 13.4, 3.25, 0.0, 0.0, 1, "unit", now, now));
                nutrientDao.insert(new NutrientEntity("Egg Yolk", 44.5, 2.36, 0.0, 3.95, 1, "unit", now, now));
                nutrientDao.insert(new NutrientEntity("Cow Milk", 67.0, 3.20, 4.40, 4.10, 100.0, "ml", now, now));
                nutrientDao.insert(new NutrientEntity("Buffalo Milk", 100.0, 3.80, 5.00, 6.90, 100.0, "ml", now, now));
                nutrientDao.insert(new NutrientEntity("Curd", 60.0, 3.20, 4.00, 3.50, 100.0, "gram", now, now));

                // --- Table 2: Grains, Oats, and Millet Varieties ---
                nutrientDao.insert(new NutrientEntity("Rice (Raw)", 365.0, 7.1, 80.0, 0.7, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Rice (Cooked)", 130.0, 2.7, 28.0, 0.3, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Wheat Flour", 340.0, 13.0, 72.0, 2.5, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Oats", 389.0, 16.9, 66.3, 6.9, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Jowar (Raw)", 329.0, 10.6, 72.1, 3.5, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Jowar (Cooked)", 119.0, 3.5, 25.0, 1.0, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Bajra (Raw)", 361.0, 11.6, 67.5, 5.0, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Bajra (Cooked)", 119.0, 3.6, 23.7, 1.2, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Ragi (Raw)", 336.0, 7.3, 72.0, 1.3, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Ragi (Cooked)", 112.0, 2.7, 23.0, 0.4, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Millet (Cooked)", 119.0, 3.5, 23.7, 1.0, 100.0, "gram", now, now));

                // --- Table 3: Indian Legumes, Whole Pulses, and Split Dals ---
                nutrientDao.insert(new NutrientEntity("Kala Chana", 329.0, 21.50, 39.56, 5.30, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Chole", 360.0, 19.00, 58.00, 6.00, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Rajma", 298.0, 19.40, 48.61, 1.70, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Matki", 309.0, 20.00, 52.09, 1.80, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Moong Dal", 327.0, 23.80, 52.59, 1.40, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Masoor Dal", 322.0, 24.30, 52.53, 0.70, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Urad Dal", 324.0, 23.10, 51.00, 1.60, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Toor Dal", 335.0, 22.30, 55.23, 1.50, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Chana Dal", 329.0, 21.50, 46.72, 5.30, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Moong", 105.0, 8.50, 16.50, 0.20, 100.0, "gram", now, now));

                // --- Table 4: Soy Products, Paneer, Tofu, and Nut Butters ---
                nutrientDao.insert(new NutrientEntity("Soyabean", 345.0, 52.00, 33.00, 1.00, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Paneer", 265.0, 18.30, 1.20, 20.80, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Tofu", 76.0, 8.10, 1.90, 4.80, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Peanut Butter", 588.0, 25.00, 20.00, 50.00, 100.0, "gram", now, now));

                // --- Table 5: Roots, Greens, and Cruciferous Vegetables ---
                nutrientDao.insert(new NutrientEntity("Potato", 70.0, 1.54, 14.89, 0.23, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Sweet Potato", 109.0, 1.20, 25.00, 0.10, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Onion", 40.0, 1.10, 9.00, 0.10, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Spinach", 23.0, 2.14, 2.05, 0.64, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Cauliflower", 30.0, 2.50, 4.00, 0.30, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Cabbage", 25.0, 1.80, 4.50, 0.10, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Green Peas", 93.0, 7.20, 11.88, 0.10, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Banana", 116.0, 1.20, 27.20, 0.30, 100.0, "gram", now, now));

                // --- Table 6: Sprouted Legumes, Common Vegetables, and Hot Beverages ---
                nutrientDao.insert(new NutrientEntity("Tomato", 20.0, 1.00, 3.50, 0.20, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Bhindi", 30.0, 1.90, 3.62, 0.20, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Methi", 32.0, 3.68, 2.17, 0.83, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Cappuccino", 45.0, 2.50, 3.50, 2.50, 100.0, "ml", now, now));
                nutrientDao.insert(new NutrientEntity("Black Coffee", 2.0, 0.10, 0.20, 0.0, 100.0, "ml", now, now));
                nutrientDao.insert(new NutrientEntity("Bread (White)", 265.0, 8.00, 53.00, 1.50, 100.0, "gram", now, now));
                nutrientDao.insert(new NutrientEntity("Bread (Brown)", 247.0, 12.50, 41.00, 4.20, 100.0, "gram", now, now));

                cacheManager.setDirty(true);
            }
        });
    }

    public interface OnNutrientsLoadedCallback {
        void onLoaded(List<NutrientEntity> nutrients);
    }
}
