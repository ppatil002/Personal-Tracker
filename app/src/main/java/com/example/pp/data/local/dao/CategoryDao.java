package com.example.pp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pp.data.local.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CategoryEntity category);

    @Update
    void update(CategoryEntity category);

    @Delete
    void delete(CategoryEntity category);

    @Query("SELECT * FROM categories ORDER BY category_id ASC")
    List<CategoryEntity> getAllCategories();

    @Query("SELECT * FROM categories WHERE parent_category_id IS NULL ORDER BY category_id ASC")
    List<CategoryEntity> getRootCategories();

    @Query("SELECT * FROM categories WHERE parent_category_id = :parentId ORDER BY category_id ASC")
    List<CategoryEntity> getSubCategories(String parentId);

    @Query("SELECT * FROM categories WHERE category_id = :id")
    CategoryEntity getCategoryById(String id);
}