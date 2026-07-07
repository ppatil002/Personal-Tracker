package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.NutritionIntakeDao;
import com.example.pp.data.local.entity.NutritionIntake;
import com.example.pp.data.local.model.GroupedNutritionIntake;
import com.example.pp.data.local.model.NutritionIntakeWithFood;
import com.example.pp.data.local.model.NutritionSummary;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NutritionRepository {
    private final NutritionIntakeDao nutritionDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public NutritionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.nutritionDao = db.nutritionIntakeDao();
    }

    public void addIntake(Long nutrientId, double quantity, long timestamp, String tags) {
        executor.execute(() -> {
            long now = System.currentTimeMillis();
            nutritionDao.insert(new NutritionIntake(nutrientId, quantity, timestamp, now, tags));
        });
    }

    public void updateIntake(NutritionIntake intake) {
        executor.execute(() -> {
            intake.updatedTimestamp = System.currentTimeMillis();
            nutritionDao.insert(intake);
        });
    }

    public void deleteIntake(NutritionIntake intake) {
        executor.execute(() -> nutritionDao.delete(intake));
    }

    public void updateIntakeTimestamp(long oldStart, long oldEnd, long newTimestamp) {
        executor.execute(() -> {
            List<NutritionIntakeWithFood> items = nutritionDao.getIntakesInRangeSync(oldStart, oldEnd);
            for (NutritionIntakeWithFood item : items) {
                item.intake.intakeTimestamp = newTimestamp;
                item.intake.updatedTimestamp = System.currentTimeMillis();
                nutritionDao.insert(item.intake);
            }
        });
    }

    public LiveData<NutritionSummary> getTodaySummary(long startOfDay) {
        return nutritionDao.getTodaySummary(startOfDay);
    }

    public LiveData<NutritionIntake> getLastIntake(long startOfDay) {
        return nutritionDao.getLastIntake(startOfDay);
    }

    public LiveData<List<NutritionIntakeWithFood>> getIntakesInRange(long start, long end) {
        return nutritionDao.getIntakesInRange(start, end);
    }

    public LiveData<List<GroupedNutritionIntake>> getGroupedIntakesInRange(long start, long end) {
        return nutritionDao.getGroupedIntakesInRange(start, end);
    }

    public List<NutritionIntakeWithFood> getIntakesInRangeSync(long start, long end) {
        return nutritionDao.getIntakesInRangeSync(start, end);
    }
}
