package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "nutrition_intake")
public class NutritionIntake {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "log_id")
    public long id;

    @ColumnInfo(name = "food_item")
    public String foodItem;

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

    public NutritionIntake(String foodItem, double calories, double carbsG, double proteinG, double fatsG, long timestamp) {
        this.foodItem = foodItem;
        this.calories = calories;
        this.carbsG = carbsG;
        this.proteinG = proteinG;
        this.fatsG = fatsG;
        this.timestamp = timestamp;
    }
}
