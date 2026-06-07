package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "nutrition_intake",
    foreignKeys = @ForeignKey(
        entity = NutrientEntity.class,
        parentColumns = "nutrient_id",
        childColumns = "nutrient_id",
        onDelete = ForeignKey.SET_NULL
    ),
    indices = {@Index("nutrient_id")}
)
public class NutritionIntake {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "log_id")
    public long id;

    @ColumnInfo(name = "nutrient_id")
    public Long nutrientId; // Can be null if food is deleted

    @ColumnInfo(name = "quantity")
    public double quantity;

    @ColumnInfo(name = "intake_timestamp")
    public long intakeTimestamp;

    @ColumnInfo(name = "updated_timestamp")
    public long updatedTimestamp;

    @ColumnInfo(name = "tags")
    public String tags;

    public NutritionIntake(Long nutrientId, double quantity, long intakeTimestamp, long updatedTimestamp, String tags) {
        this.nutrientId = nutrientId;
        this.quantity = quantity;
        this.intakeTimestamp = intakeTimestamp;
        this.updatedTimestamp = updatedTimestamp;
        this.tags = tags;
    }
}
