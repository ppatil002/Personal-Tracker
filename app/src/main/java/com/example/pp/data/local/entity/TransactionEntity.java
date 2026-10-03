package com.example.pp.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(
    tableName = "transactions",
    foreignKeys = @ForeignKey(
        entity = CategoryEntity.class,
        parentColumns = "category_id",
        childColumns = "category_id",
        onDelete = ForeignKey.RESTRICT
    ),
    indices = {
        @Index("category_id"),
        @Index("transaction_date")
    }
)
public class TransactionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "transaction_id")
    private String transactionId;

    @NonNull
    @ColumnInfo(name = "category_id")
    private String categoryId;

    @NonNull
    @ColumnInfo(name = "item")
    private String item;

    @ColumnInfo(name = "amount")
    private double amount;

    @ColumnInfo(name = "notes")
    private String notes;

    @ColumnInfo(name = "transaction_date")
    private long transactionDate;

    @ColumnInfo(name = "created_timestamp")
    private long createdTimestamp;

    @ColumnInfo(name = "last_updated_timestamp")
    private long lastUpdatedTimestamp;

    @ColumnInfo(name = "payment_method")
    private String paymentMethod;

    @NonNull
    @ColumnInfo(name = "transaction_type")
    private String transactionType;

    public TransactionEntity(@NonNull String transactionId, @NonNull String categoryId, @NonNull String item, 
                             double amount, String notes, long transactionDate, 
                             long createdTimestamp, long lastUpdatedTimestamp, String paymentMethod, @NonNull String transactionType) {
        this.transactionId = transactionId;
        this.categoryId = categoryId;
        this.item = item;
        this.amount = amount;
        this.notes = notes;
        this.transactionDate = transactionDate;
        this.createdTimestamp = createdTimestamp;
        this.lastUpdatedTimestamp = lastUpdatedTimestamp;
        this.paymentMethod = paymentMethod;
        this.transactionType = transactionType;
    }
}