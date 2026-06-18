package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "body_weight")
public class BodyWeight {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "weight")
    public double weight;

    @ColumnInfo(name = "date_measured")
    public long dateMeasured;

    public BodyWeight(double weight, long dateMeasured) {
        this.weight = weight;
        this.dateMeasured = dateMeasured;
    }
}
