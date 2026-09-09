package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.pp.data.local.entity.RegistryEntity;
import com.example.pp.data.local.model.SyncStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Dao
public interface RegistryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(RegistryEntity registry);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBatch(List<RegistryEntity> items);

    @Update
    void updateBatch(List<RegistryEntity> items);

    @Query("SELECT * FROM registry_store WHERE item = :item LIMIT 1")
    RegistryEntity getRegistryItem(String item);

    @Query("SELECT * FROM registry_store WHERE item = :item LIMIT 1")
    LiveData<RegistryEntity> getRegistryItemLive(String item);

    @Query("SELECT * FROM registry_store")
    LiveData<List<RegistryEntity>> getAllItems();

    @Query("SELECT * FROM registry_store")
    List<RegistryEntity> getAllItemsList();

    @Query("SELECT COUNT(*) FROM registry_store")
    int getCount();

    @Transaction
    default SyncStats upsertAll(List<RegistryEntity> cloudItems) {
        SyncStats stats = new SyncStats("Registry");
        stats.previousCount = getCount();
        if (cloudItems == null || cloudItems.isEmpty()) {
            stats.finalCount = stats.previousCount;
            return stats;
        }

        List<RegistryEntity> localItems = getAllItemsList();
        Map<String, String> localMap = new HashMap<>();
        for (RegistryEntity local : localItems) {
            localMap.put(local.item, String.valueOf(local.lastUpdatedTimestamp));
        }

        List<RegistryEntity> toInsert = new ArrayList<>();
        List<RegistryEntity> toUpdate = new ArrayList<>();

        for (RegistryEntity cloud : cloudItems) {
            String localTs = localMap.get(cloud.item);
            if (localTs == null) {
                toInsert.add(cloud);
            } else if (cloud.lastUpdatedTimestamp > Long.parseLong(localTs)) {
                toUpdate.add(cloud);
            }
        }

        if (!toInsert.isEmpty()) insertBatch(toInsert);
        if (!toUpdate.isEmpty()) updateBatch(toUpdate);

        stats.inserted = toInsert.size();
        stats.updated = toUpdate.size();
        stats.finalCount = getCount();
        return stats;
    }
}
