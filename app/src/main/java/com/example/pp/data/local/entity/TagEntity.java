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

    public TagEntity(@NonNull String tagId, String name) {
        this.tagId = tagId;
        this.name = name;
    }

    @NonNull
    public String getTagId() { return tagId; }
    public String getName() { return name; }

    public void setTagId(@NonNull String tagId) { this.tagId = tagId; }
    public void setName(String name) { this.name = name; }
}