package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    public long getNutrientId() { return nutrientId; }
    public void setNutrientId(long nutrientId) { this.nutrientId = nutrientId; }

    public String getFoodItem() { return foodItem; }
    public void setFoodItem(String foodItem) { this.foodItem = foodItem; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getCarbs() { return carbs; }
    public void setCarbs(double carbs) { this.carbs = carbs; }

    public double getFats() { return fats; }
    public void setFats(double fats) { this.fats = fats; }

    public double getPerUnit() { return perUnit; }
    public void setPerUnit(double perUnit) { this.perUnit = perUnit; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public long getCreatedDate() { return createdDate; }
    public void setCreatedDate(long createdDate) { this.createdDate = createdDate; }

    public long getUpdatedDate() { return updatedDate; }
    public void setUpdatedDate(long updatedDate) { this.updatedDate = updatedDate; }
}
