package com.example.pp.features.analytics.model;

public class AnalyticsSummary {
    private double totalExpenses;
    private int transactionCount;
    private double avgPerDay;
    private String highestCategoryName;
    private String highestCategoryColour;
    private double highestCategoryAmount;

    public AnalyticsSummary(double totalExpenses, int transactionCount, double avgPerDay,
                              String highestCategoryName, String highestCategoryColour, double highestCategoryAmount) {
        this.totalExpenses = totalExpenses;
        this.transactionCount = transactionCount;
        this.avgPerDay = avgPerDay;
        this.highestCategoryName = highestCategoryName;
        this.highestCategoryColour = highestCategoryColour;
        this.highestCategoryAmount = highestCategoryAmount;
    }

    public double getTotalExpenses() { return totalExpenses; }
    public int getTransactionCount() { return transactionCount; }
    public double getAvgPerDay() { return avgPerDay; }
    public String getHighestCategoryName() { return highestCategoryName; }
    public String getHighestCategoryColour() { return highestCategoryColour; }
    public double getHighestCategoryAmount() { return highestCategoryAmount; }
}
