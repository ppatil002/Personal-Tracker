package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(
    tableName = "categories",
    foreignKeys = @ForeignKey(
        entity = CategoryEntity.class,
        parentColumns = "category_id",
        childColumns = "parent_category_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("parent_category_id")}
)
public class CategoryEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "category_id")
    private String categoryId;

    @ColumnInfo(name = "parent_category_id")
    private String parentCategoryId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "icon")
    private String icon; // Emoji string

    @ColumnInfo(name = "colour")
    private String colour; // Hex string

    @ColumnInfo(name = "last_updated_timestamp")
    private long lastUpdatedTimestamp;

    public CategoryEntity(@NonNull String categoryId, String parentCategoryId, String name, 
                          String icon, String colour, long lastUpdatedTimestamp) {
        this.categoryId = categoryId;
        this.parentCategoryId = parentCategoryId;
        this.name = name;
        this.icon = icon;
        this.colour = colour;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }
}