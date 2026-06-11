package com.example.pp.data.local.model;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.pp.data.local.entity.NutrientEntity;
import com.example.pp.data.local.entity.NutritionIntake;

public class NutritionIntakeWithFood {
    @Embedded
    public NutritionIntake intake;

    @Relation(
        parentColumn = "nutrient_id",
        entityColumn = "nutrient_id"
    )
    public NutrientEntity food;
}
