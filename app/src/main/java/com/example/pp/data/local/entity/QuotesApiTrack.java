package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quotes_api_track")
public class QuotesApiTrack {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "quote")
    public String quote;

    @ColumnInfo(name = "author")
    public String author;

    @ColumnInfo(name = "work")
    public String work;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "fetch_timestamp")
    public long fetchTimestamp;

    public QuotesApiTrack(String quote, String author, String work, String category, long fetchTimestamp) {
        this.quote = quote;
        this.author = author;
        this.work = work;
        this.category = category;
        this.fetchTimestamp = fetchTimestamp;
    }
}
