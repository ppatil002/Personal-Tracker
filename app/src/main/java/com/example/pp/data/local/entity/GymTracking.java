package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gym_tracking")
public class GymTracking {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "muscle")
    public String muscle;

    @ColumnInfo(name = "exercise")
    public String exercise;

    @ColumnInfo(name = "sets_json")
    public String setsJson; // Stores a list of sets: reps, weight, difficulty

    @ColumnInfo(name = "difficulty")
    public String difficulty;

    @ColumnInfo(name = "exercise_date")
    public long exerciseDate;

    public GymTracking(String muscle, String exercise, String setsJson, String difficulty, long exerciseDate) {
        this.muscle = muscle;
        this.exercise = exercise;
        this.setsJson = setsJson;
        this.difficulty = difficulty;
        this.exerciseDate = exerciseDate;
    }
}
