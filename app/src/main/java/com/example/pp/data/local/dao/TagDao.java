package com.example.pp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.pp.data.local.entity.TagEntity;

import java.util.List;

@Dao
public interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TagEntity tag);

    @Update
    void update(TagEntity tag);

    @Delete
    void delete(TagEntity tag);

    @Query("SELECT * FROM tags ORDER BY name ASC")
    List<TagEntity> getAllTagsList();

    @Query("SELECT * FROM tags ORDER BY name ASC")
    List<TagEntity> getAllTags();

    @Query("SELECT * FROM tags WHERE tag_id = :id")
    TagEntity getTagById(String id);

    @Query("SELECT MAX(CAST(tag_id AS INTEGER)) FROM tags")
    Integer getMaxTagId();
}