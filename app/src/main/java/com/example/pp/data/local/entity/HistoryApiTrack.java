package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "history_api_track")
public class HistoryApiTrack {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "history")
    public String history;

    @ColumnInfo(name = "history_date")
    public String historyDate;

    @ColumnInfo(name = "fetch_timestamp")
    public long fetchTimestamp;

    public HistoryApiTrack(String history, String historyDate, long fetchTimestamp) {
        this.history = history;
        this.historyDate = historyDate;
        this.fetchTimestamp = fetchTimestamp;
    }
}
