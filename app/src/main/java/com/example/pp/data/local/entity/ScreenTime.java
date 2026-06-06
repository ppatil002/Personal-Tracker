package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

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

    @NonNull
    public String getPackageName() { return packageName; }
    public void setPackageName(@NonNull String packageName) { this.packageName = packageName; }

    public long getHourStartTimestamp() { return hourStartTimestamp; }
    public void setHourStartTimestamp(long hourStartTimestamp) { this.hourStartTimestamp = hourStartTimestamp; }

    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }

    public int getTimesOpened() { return timesOpened; }
    public void setTimesOpened(int timesOpened) { this.timesOpened = timesOpened; }

    public int getNotificationsReceived() { return notificationsReceived; }
    public void setNotificationsReceived(int notificationsReceived) { this.notificationsReceived = notificationsReceived; }
}
