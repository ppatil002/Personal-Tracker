package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(tableName = "background_job_log")
public class BackgroundJobLog {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "job_id")
    private String jobId;

    @NonNull
    @ColumnInfo(name = "worker_name")
    private String workerName;

    @NonNull
    @ColumnInfo(name = "status")
    private String status; // PENDING, RUNNING, SUCCESS, FAILED, STALLED

    @ColumnInfo(name = "current_step")
    private String currentStep;

    @ColumnInfo(name = "log_message")
    private String logMessage;

    @ColumnInfo(name = "last_updated_timestamp")
    private long lastUpdatedTimestamp;

    public BackgroundJobLog(@NonNull String jobId, @NonNull String workerName, @NonNull String status, 
                             String currentStep, String logMessage, long lastUpdatedTimestamp) {
        this.jobId = jobId;
        this.workerName = workerName;
        this.status = status;
        this.currentStep = currentStep;
        this.logMessage = logMessage;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }
}