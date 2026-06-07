package com.example.pp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.pp.data.local.entity.NutrientEntity;
import com.example.pp.data.local.model.SyncStats;

import java.util.List;

@Dao
public interface NutrientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NutrientEntity nutrient);

    @Update
    void update(NutrientEntity nutrient);

    @Delete
    void delete(NutrientEntity nutrient);

    @Query("SELECT * FROM nutrient_data ORDER BY food_item ASC")
    List<NutrientEntity> getAll();

    @Query("SELECT * FROM nutrient_data WHERE nutrient_id = :id LIMIT 1")
    NutrientEntity getById(long id);

    @Query("SELECT * FROM nutrient_data WHERE food_item = :name LIMIT 1")
    NutrientEntity getByName(String name);

    @Query("SELECT COUNT(*) FROM nutrient_data")
    int getCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBatch(List<NutrientEntity> nutrients);

    @Transaction
    default SyncStats upsertAll(List<NutrientEntity> items) {
        SyncStats stats = new SyncStats("NutrientData");
        stats.previousCount = getCount();
        if (items != null && !items.isEmpty()) {
            insertBatch(items);
            stats.inserted = items.size();
        }
        stats.finalCount = getCount();
        return stats;
    }

    @Query("SELECT * FROM nutrient_data")
    List<NutrientEntity> getAllList();
}
