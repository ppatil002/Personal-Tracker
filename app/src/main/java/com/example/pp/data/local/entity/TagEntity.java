package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tags")
public class TagEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "tag_id")
    private String tagId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "last_updated_timestamp")
    private long lastUpdatedTimestamp;

    public TagEntity(@NonNull String tagId, String name, long lastUpdatedTimestamp) {
        this.tagId = tagId;
        this.name = name;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    @NonNull
    public String getTagId() { return tagId; }
    public String getName() { return name; }
    public long getLastUpdatedTimestamp() { return lastUpdatedTimestamp; }

    public void setTagId(@NonNull String tagId) { this.tagId = tagId; }
    public void setName(String name) { this.name = name; }
    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) { this.lastUpdatedTimestamp = lastUpdatedTimestamp; }
}