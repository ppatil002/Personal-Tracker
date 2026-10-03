package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(tableName = "screen_time",
        primaryKeys = {"packageName", "hourStartTimestamp"})
public class ScreenTime {
    
    @NonNull
    private String packageName;
    
    private long hourStartTimestamp;
    
    private long durationMillis;
    
    private int timesOpened;
    
    private int notificationsReceived;

    public ScreenTime(@NonNull String packageName, long hourStartTimestamp, long durationMillis) {
        this.packageName = packageName;
        this.hourStartTimestamp = hourStartTimestamp;
        this.durationMillis = durationMillis;
    }
}
