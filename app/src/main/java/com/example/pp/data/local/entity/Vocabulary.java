package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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

    @ColumnInfo(name = "last_reviewed_timestamp")
    public long lastReviewedTimestamp;

    @ColumnInfo(name = "correct_attempts")
    public int correctAttempts;

    @ColumnInfo(name = "incorrect_attempts")
    public int incorrectAttempts;

    @ColumnInfo(name = "hints_used_count")
    public int hintsUsedCount;

    @ColumnInfo(name = "half_life")
    public double halfLife;

    @ColumnInfo(name = "next_revision_timestamp")
    public long nextRevisionTimestamp;

    public Vocabulary() {
    }

    @Ignore
    public Vocabulary(String word, String meaning, String example, String partOfSpeech, long createdTimestamp) {
        this.word = word;
        this.meaning = meaning;
        this.example = example;
        this.partOfSpeech = partOfSpeech;
        this.createdTimestamp = createdTimestamp;
        this.lastReviewedTimestamp = 0;
        this.correctAttempts = 0;
        this.incorrectAttempts = 0;
        this.hintsUsedCount = 0;
        this.halfLife = 0.0;
        this.nextRevisionTimestamp = 0;
    }

    @Ignore
    public Vocabulary(String word, String meaning, String example, String partOfSpeech, long createdTimestamp,
                      long lastReviewedTimestamp, int correctAttempts, int incorrectAttempts,
                      int hintsUsedCount, double halfLife, long nextRevisionTimestamp) {
        this.word = word;
        this.meaning = meaning;
        this.example = example;
        this.partOfSpeech = partOfSpeech;
        this.createdTimestamp = createdTimestamp;
        this.lastReviewedTimestamp = lastReviewedTimestamp;
        this.correctAttempts = correctAttempts;
        this.incorrectAttempts = incorrectAttempts;
        this.hintsUsedCount = hintsUsedCount;
        this.halfLife = halfLife;
        this.nextRevisionTimestamp = nextRevisionTimestamp;
    }
}
