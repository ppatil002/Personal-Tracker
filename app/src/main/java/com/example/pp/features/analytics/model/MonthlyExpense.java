package com.example.pp.features.analytics.model;

public class MonthlyExpense {
    private int month; // 1-12
    private double totalAmount;

    public MonthlyExpense(int month, double totalAmount) {
        this.month = month;
        this.totalAmount = totalAmount;
    }

    public int getMonth() { return month; }
    public double getTotalAmount() { return totalAmount; }
}
