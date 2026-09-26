package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "vocabulary", indices = {@Index(value = {"word"}, unique = true)})
public class Vocabulary {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "word")
    public String word;

    @ColumnInfo(name = "meaning")
    public String meaning;

    @ColumnInfo(name = "example")
    public String example;

    @ColumnInfo(name = "part_of_speach")
    public String partOfSpeech;

    @ColumnInfo(name = "created_timestamp")
    public long createdTimestamp;

    public Vocabulary(String word, String meaning, String example, String partOfSpeech, long createdTimestamp) {
        this.word = word;
        this.meaning = meaning;
        this.example = example;
        this.partOfSpeech = partOfSpeech;
        this.createdTimestamp = createdTimestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
}
