package com.example.pp.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.pp.data.local.entity.BackgroundJobLog;

import java.util.List;

@Dao
public interface BackgroundJobLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BackgroundJobLog log);

    @Query("SELECT * FROM background_job_log ORDER BY last_updated_timestamp DESC")
    LiveData<List<BackgroundJobLog>> getAllLogs();

    @Query("SELECT * FROM background_job_log WHERE job_id = :jobId")
    BackgroundJobLog getLogById(String jobId);

    @Query("DELETE FROM background_job_log")
    void deleteAll();
}