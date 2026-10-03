package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.pp.core.enums.TaskStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(tableName = "task_states")
public class TaskState {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "task_id")
    private String taskId;

    @NonNull
    @ColumnInfo(name = "current_state")
    private TaskStatus currentState;

    @ColumnInfo(name = "last_run_time")
    private long lastRunTime;

    @ColumnInfo(name = "next_run_time")
    private long nextRunTime;

    @ColumnInfo(name = "paused_at")
    private long pausedAt;

    @ColumnInfo(name = "snoozed_until")
    private long snoozedUntil;

    @ColumnInfo(name = "retry_count")
    private int retryCount;

    @ColumnInfo(name = "last_failure_reason")
    private String lastFailureReason;

    @ColumnInfo(name = "awaiting_user_action")
    private boolean awaitingUserAction;

    @ColumnInfo(name = "is_running")
    private boolean isRunning;

    @ColumnInfo(name = "progress")
    private int progress;

    public TaskState(@NonNull String taskId, @NonNull TaskStatus currentState, long lastRunTime, 
                     long nextRunTime, long pausedAt, long snoozedUntil, int retryCount, 
                     String lastFailureReason, boolean awaitingUserAction, boolean isRunning, int progress) {
        this.taskId = taskId;
        this.currentState = currentState;
        this.lastRunTime = lastRunTime;
        this.nextRunTime = nextRunTime;
        this.pausedAt = pausedAt;
        this.snoozedUntil = snoozedUntil;
        this.retryCount = retryCount;
        this.lastFailureReason = lastFailureReason;
        this.awaitingUserAction = awaitingUserAction;
        this.isRunning = isRunning;
        this.progress = progress;
    }
}
