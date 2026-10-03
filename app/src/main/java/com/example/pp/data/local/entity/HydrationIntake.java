package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(tableName = "hydration_intakes")
public class HydrationIntake {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "amount_ml")
    public int amountMl;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    public HydrationIntake(int amountMl, long timestamp) {
        this.amountMl = amountMl;
        this.timestamp = timestamp;
    }
}
