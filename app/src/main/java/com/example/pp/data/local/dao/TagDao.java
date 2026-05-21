package com.example.pp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.pp.data.local.entity.TagEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dao
public interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(TagEntity tag);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertBatch(List<TagEntity> tags);

    @Update
    void update(TagEntity tag);

    @Update
    void updateBatch(List<TagEntity> tags);

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

    @Transaction
    default void upsertAll(List<TagEntity> cloudItems) {
        if (cloudItems == null || cloudItems.isEmpty()) return;

        List<TagEntity> localItems = getAllTagsList();
        Map<String, TagEntity> localMap = new HashMap<>();
        for (TagEntity local : localItems) {
            localMap.put(local.getTagId(), local);
        }

        List<TagEntity> toInsert = new ArrayList<>();
        List<TagEntity> toUpdate = new ArrayList<>();

        for (TagEntity cloud : cloudItems) {
            TagEntity local = localMap.get(cloud.getTagId());
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
