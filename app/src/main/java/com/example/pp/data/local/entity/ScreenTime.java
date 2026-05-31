package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "screen_time")
public class ScreenTime {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @NonNull
    private String packageName;
    
    private long hourStartTimestamp;
    
    private long durationMillis;

    public ScreenTime(@NonNull String packageName, long hourStartTimestamp, long durationMillis) {
        this.packageName = packageName;
        this.hourStartTimestamp = hourStartTimestamp;
        this.durationMillis = durationMillis;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    @NonNull
    public String getPackageName() { return packageName; }
    public void setPackageName(@NonNull String packageName) { this.packageName = packageName; }

    public long getHourStartTimestamp() { return hourStartTimestamp; }
    public void setHourStartTimestamp(long hourStartTimestamp) { this.hourStartTimestamp = hourStartTimestamp; }

    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }
}
