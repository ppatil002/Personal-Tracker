package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saved_quotes")
public class SavedQuote {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "quote")
    public String quote;

    @ColumnInfo(name = "author")
    public String author;

    @ColumnInfo(name = "category")
    public String category;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "created_date")
    public long createdDate;

    public SavedQuote(String quote, String author, String category, String notes, long createdDate) {
        this.quote = quote;
        this.author = author;
        this.category = category;
        this.notes = notes;
        this.createdDate = createdDate;
    }
}
