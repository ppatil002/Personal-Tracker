package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    @ColumnInfo(name = "valid_from")
    private Long validFrom;

    @ColumnInfo(name = "valid_till")
    private Long validTill;

    public TagEntity(@NonNull String tagId, String name, long lastUpdatedTimestamp) {
        this.tagId = tagId;
        this.name = name;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }
}