package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(tableName = "nutrient_data")
public class NutrientEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "nutrient_id")
    private long nutrientId;

    @ColumnInfo(name = "food_item")
    private String foodItem;

    @ColumnInfo(name = "calories")
    private double calories;

    @ColumnInfo(name = "protein")
    private double protein;

    @ColumnInfo(name = "carbs")
    private double carbs;

    @ColumnInfo(name = "fats")
    private double fats;

    @ColumnInfo(name = "per_unit")
    private double perUnit;

    @ColumnInfo(name = "unit")
    private String unit; // gram, ml, quantity

    @ColumnInfo(name = "created_date")
    private long createdDate;

    @ColumnInfo(name = "updated_date")
    private long updatedDate;

    public NutrientEntity(@NonNull String foodItem, double calories, double protein, double carbs, double fats, double perUnit, String unit, long createdDate, long updatedDate) {
        this.foodItem = foodItem;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
        this.perUnit = perUnit;
        this.unit = unit;
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }
}
