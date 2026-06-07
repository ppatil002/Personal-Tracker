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

    @ColumnInfo(name = "calories")
    public double calories;

    @ColumnInfo(name = "carbs_g")
    public double carbsG;

    @ColumnInfo(name = "protein_g")
    public double proteinG;

    @ColumnInfo(name = "fats_g")
    public double fatsG;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "tags")
    public String tags;

    public NutritionIntake(Long nutrientId, double calories, double carbsG, double proteinG, double fatsG, long timestamp, String tags) {
        this.nutrientId = nutrientId;
        this.calories = calories;
        this.carbsG = carbsG;
        this.proteinG = proteinG;
        this.fatsG = fatsG;
        this.timestamp = timestamp;
        this.tags = tags;
    }
}
