package com.example.pp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.pp.data.local.entity.CategoryEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertBatch(List<CategoryEntity> categories);

    @Update
    void update(CategoryEntity category);

    @Update
    void updateBatch(List<CategoryEntity> categories);

    @Delete
    void delete(CategoryEntity category);

    @Query("SELECT * FROM categories ORDER BY category_id ASC")
    List<CategoryEntity> getAllCategoriesList();

    @Query("SELECT * FROM categories ORDER BY category_id ASC")
    List<CategoryEntity> getAllCategories();

    @Query("SELECT * FROM categories WHERE parent_category_id IS NULL ORDER BY category_id ASC")
    List<CategoryEntity> getRootCategories();

    @Query("SELECT * FROM categories WHERE parent_category_id = :parentId ORDER BY category_id ASC")
    List<CategoryEntity> getSubCategories(String parentId);

    @Query("SELECT * FROM categories WHERE category_id = :id")
    CategoryEntity getCategoryById(String id);

    @Query("SELECT MAX(CAST(category_id AS INTEGER)) FROM categories WHERE parent_category_id IS NULL")
    Integer getMaxRootId();

    @Query("SELECT MAX(CAST(category_id AS REAL)) FROM categories WHERE parent_category_id = :parentId")
    Double getMaxChildId(String parentId);

    @Transaction
    default void upsertAll(List<CategoryEntity> cloudItems) {
        if (cloudItems == null || cloudItems.isEmpty()) return;

        List<CategoryEntity> localItems = getAllCategoriesList();
        Map<String, CategoryEntity> localMap = new HashMap<>();
        for (CategoryEntity local : localItems) {
            localMap.put(local.getCategoryId(), local);
        }

        List<CategoryEntity> toInsert = new ArrayList<>();
        List<CategoryEntity> toUpdate = new ArrayList<>();

        for (CategoryEntity cloud : cloudItems) {
            CategoryEntity local = localMap.get(cloud.getCategoryId());
            if (local == null) {
                toInsert.add(cloud);
            } else if (cloud.getLastUpdatedTimestamp() > local.getLastUpdatedTimestamp()) {
                toUpdate.add(cloud);
            }
        }

        if (!toInsert.isEmpty()) insertBatch(toInsert);
        if (!toUpdate.isEmpty()) updateBatch(toUpdate);
    }
}
