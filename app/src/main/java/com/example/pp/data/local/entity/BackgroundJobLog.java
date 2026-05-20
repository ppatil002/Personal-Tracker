package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

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

    @NonNull
    public String getJobId() { return jobId; }
    @NonNull
    public String getWorkerName() { return workerName; }
    @NonNull
    public String getStatus() { return status; }
    public String getCurrentStep() { return currentStep; }
    public String getLogMessage() { return logMessage; }
    public long getLastUpdatedTimestamp() { return lastUpdatedTimestamp; }

    public void setJobId(@NonNull String jobId) { this.jobId = jobId; }
    public void setWorkerName(@NonNull String workerName) { this.workerName = workerName; }
    public void setStatus(@NonNull String status) { this.status = status; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public void setLogMessage(String logMessage) { this.logMessage = logMessage; }
    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) { this.lastUpdatedTimestamp = lastUpdatedTimestamp; }
}