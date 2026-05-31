package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sleep")
public class SleepEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "start_time")
    public long startTime;

    @ColumnInfo(name = "end_time")
    public long endTime;

    @ColumnInfo(name = "target_wake_time")
    public long targetWakeTime;

    @ColumnInfo(name = "date")
    public long date; // Start of day for tracking

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public SleepEntity() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public SleepEntity(long startTime, long endTime, long targetWakeTime, long date) {
        this();
        this.startTime = startTime;
        this.endTime = endTime;
        this.targetWakeTime = targetWakeTime;
        this.date = date;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { 
        this.startTime = startTime; 
        this.updatedAt = System.currentTimeMillis();
    }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { 
        this.endTime = endTime; 
        this.updatedAt = System.currentTimeMillis();
    }

    public long getTargetWakeTime() { return targetWakeTime; }
    public void setTargetWakeTime(long targetWakeTime) { 
        this.targetWakeTime = targetWakeTime; 
        this.updatedAt = System.currentTimeMillis();
    }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
