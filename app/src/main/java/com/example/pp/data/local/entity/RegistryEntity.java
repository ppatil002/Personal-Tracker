package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "registry_store",
    indices = {@Index(value = {"item"}, unique = true)}
)
public class RegistryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "item")
    public String item;

    @ColumnInfo(name = "value")
    public String value;

    @ColumnInfo(name = "last_updated_timestamp")
    public long lastUpdatedTimestamp;

    public RegistryEntity(@NonNull String item, String value, long lastUpdatedTimestamp) {
        this.item = item;
        this.value = value;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }
}
