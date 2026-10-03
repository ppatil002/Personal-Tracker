package com.example.pp.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(
    tableName = "vocabulary_revision_logs",
    foreignKeys = @ForeignKey(
        entity = Vocabulary.class,
        parentColumns = "id",
        childColumns = "vocabulary_id",
        onDelete = ForeignKey.CASCADE
    ), indices = {
                @Index("vocabulary_id")
        }
)
public class VocabularyRevisionLog {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "vocabulary_id")
    public long vocabularyId;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    @ColumnInfo(name = "action")
    public String action;

    @ColumnInfo(name = "hint_used")
    public boolean hintUsed;

    @ColumnInfo(name = "rating")
    public int rating;

    @ColumnInfo(name = "half_life_before")
    public double halfLifeBefore;

    @ColumnInfo(name = "half_life_after")
    public double halfLifeAfter;

    public VocabularyRevisionLog() {
    }

    @Ignore
    public VocabularyRevisionLog(long vocabularyId, long timestamp, String action, boolean hintUsed,
                                 int rating, double halfLifeBefore, double halfLifeAfter) {
        this.vocabularyId = vocabularyId;
        this.timestamp = timestamp;
        this.action = action;
        this.hintUsed = hintUsed;
        this.rating = rating;
        this.halfLifeBefore = halfLifeBefore;
        this.halfLifeAfter = halfLifeAfter;
    }
}
