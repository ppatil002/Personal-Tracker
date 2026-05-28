package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.example.pp.data.local.entity.NutritionIntake;
import com.example.pp.data.local.model.NutritionSummary;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface NutritionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NutritionIntake log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBatch(List<NutritionIntake> logs);

    @Query("SELECT COUNT(*) FROM nutrition_intake")
    int getCount();

    @Transaction
    default SyncStats upsertAll(List<NutritionIntake> items) {
        SyncStats stats = new SyncStats("Nutrition");
        stats.previousCount = getCount();
        if (items != null && !items.isEmpty()) {
            insertBatch(items);
            stats.inserted = items.size();
        }
        stats.finalCount = getCount();
        return stats;
    }

    @Query("SELECT SUM(calories) as totalCalories, SUM(carbs_g) as totalCarbs, SUM(protein_g) as totalProtein, SUM(fats_g) as totalFats FROM nutrition_intake WHERE timestamp >= :startOfDay")
    LiveData<NutritionSummary> getTodaySummary(long startOfDay);

    @Query("SELECT * FROM nutrition_intake")
    List<NutritionIntake> getAllIntakes();

    @Query("SELECT * FROM nutrition_intake WHERE timestamp >= :startOfDay ORDER BY timestamp DESC LIMIT 1")
    LiveData<NutritionIntake> getLastIntake(long startOfDay);
}
