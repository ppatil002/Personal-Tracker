package com.example.pp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.NutritionDao;
import com.example.pp.data.local.entity.NutritionIntake;
import com.example.pp.data.local.model.NutritionSummary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NutritionRepository {
    private final NutritionDao nutritionDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public NutritionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.nutritionDao = db.nutritionDao();
    }

    public void addIntake(String foodItem, double calories, double carbs, double protein, double fats, long timestamp) {
        executor.execute(() -> {
            nutritionDao.insert(new NutritionIntake(foodItem, calories, carbs, protein, fats, timestamp));
        });
    }

    public LiveData<NutritionSummary> getTodaySummary(long startOfDay) {
        return nutritionDao.getTodaySummary(startOfDay);
    }
}
