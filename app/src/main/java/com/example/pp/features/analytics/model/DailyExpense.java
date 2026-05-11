package com.example.pp.features.analytics.model;

public class DailyExpense {
    private long dateTimestamp;
    private double amount;

    public DailyExpense(long dateTimestamp, double amount) {
        this.dateTimestamp = dateTimestamp;
        this.amount = amount;
    }

    public long getDateTimestamp() { return dateTimestamp; }
    public double getAmount() { return amount; }
}
