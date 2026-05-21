package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

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

    @NonNull
    public String getCategoryId() { return categoryId; }
    public String getParentCategoryId() { return parentCategoryId; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public String getColour() { return colour; }
    public long getLastUpdatedTimestamp() { return lastUpdatedTimestamp; }

    public void setCategoryId(@NonNull String categoryId) { this.categoryId = categoryId; }
    public void setParentCategoryId(String parentCategoryId) { this.parentCategoryId = parentCategoryId; }
    public void setName(String name) { this.name = name; }
    public void setIcon(String icon) { this.icon = icon; }
    public void setColour(String colour) { this.colour = colour; }
    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) { this.lastUpdatedTimestamp = lastUpdatedTimestamp; }
}