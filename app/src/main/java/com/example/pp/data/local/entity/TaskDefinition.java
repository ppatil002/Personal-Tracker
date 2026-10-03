package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.pp.core.enums.TaskType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(tableName = "task_definitions")
public class TaskDefinition {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "task_id")
    private String taskId;

    @NonNull
    @ColumnInfo(name = "task_type")
    private TaskType taskType;

    @ColumnInfo(name = "enabled")
    private boolean enabled;

    @ColumnInfo(name = "repeat_interval")
    private long repeatInterval;

    @ColumnInfo(name = "priority")
    private int priority;

    @ColumnInfo(name = "requires_internet")
    private boolean requiresInternet;

    @ColumnInfo(name = "minimum_battery")
    private int minimumBattery;

    @ColumnInfo(name = "foreground_required")
    private boolean foregroundRequired;

    @ColumnInfo(name = "snooze_interval_minutes")
    private int snoozeIntervalMinutes;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    public TaskDefinition(@NonNull String taskId, @NonNull TaskType taskType, boolean enabled,
                          long repeatInterval, int priority, boolean requiresInternet, 
                          int minimumBattery, boolean foregroundRequired, int snoozeIntervalMinutes,
                          long createdAt, long updatedAt) {
        this.taskId = taskId;
        this.taskType = taskType;
        this.enabled = enabled;
        this.repeatInterval = repeatInterval;
        this.priority = priority;
        this.requiresInternet = requiresInternet;
        this.minimumBattery = minimumBattery;
        this.foregroundRequired = foregroundRequired;
        this.snoozeIntervalMinutes = snoozeIntervalMinutes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
