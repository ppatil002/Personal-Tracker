package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    tableName = "transaction_tag_cross_ref",
    primaryKeys = {"transaction_id", "tag_id"},
    foreignKeys = {
        @ForeignKey(
            entity = TransactionEntity.class,
            parentColumns = "transaction_id",
            childColumns = "transaction_id",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = TagEntity.class,
            parentColumns = "tag_id",
            childColumns = "tag_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index("transaction_id"),
        @Index("tag_id")
    }
)
public class TransactionTagCrossRef {

    @NonNull
    @ColumnInfo(name = "transaction_id")
    private String transactionId;

    @NonNull
    @ColumnInfo(name = "tag_id")
    private String tagId;

    @ColumnInfo(name = "last_updated_timestamp")
    private long lastUpdatedTimestamp;

    public TransactionTagCrossRef(@NonNull String transactionId, @NonNull String tagId, long lastUpdatedTimestamp) {
        this.transactionId = transactionId;
        this.tagId = tagId;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
    }

    @NonNull
    public String getTransactionId() { return transactionId; }
    @NonNull
    public String getTagId() { return tagId; }
    public long getLastUpdatedTimestamp() { return lastUpdatedTimestamp; }

    public void setTransactionId(@NonNull String transactionId) { this.transactionId = transactionId; }
    public void setTagId(@NonNull String tagId) { this.tagId = tagId; }
    public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) { this.lastUpdatedTimestamp = lastUpdatedTimestamp; }
}