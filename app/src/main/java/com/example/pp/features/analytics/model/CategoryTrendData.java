package com.example.pp.features.analytics.model;

import java.util.List;

public class CategoryTrendData {
    private String categoryId;
    private String categoryName;
    private String categoryIcon;
    private String categoryColor;
    private List<DailyExpense> dailyTrend; // Current month daily data
    private List<MonthlyTotal> historicalTotals; // Last 3 months totals
    private List<MonthlyTotal> yearlyTrend; // Jan-Dec totals

    public CategoryTrendData(String categoryId, String categoryName, String categoryIcon, String categoryColor) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryIcon = categoryIcon;
        this.categoryColor = categoryColor;
    }

    public String getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryIcon() { return categoryIcon; }
    public String getCategoryColor() { return categoryColor; }
    
    public List<DailyExpense> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<DailyExpense> dailyTrend) { this.dailyTrend = dailyTrend; }
    
    public List<MonthlyTotal> getHistoricalTotals() { return historicalTotals; }
    public void setHistoricalTotals(List<MonthlyTotal> historicalTotals) { this.historicalTotals = historicalTotals; }
    
    public List<MonthlyTotal> getYearlyTrend() { return yearlyTrend; }
    public void setYearlyTrend(List<MonthlyTotal> yearlyTrend) { this.yearlyTrend = yearlyTrend; }

    public static class MonthlyTotal {
        private long timestamp;
        private String label;
        private double amount;

        public MonthlyTotal(long timestamp, String label, double amount) {
            this.timestamp = timestamp;
            this.label = label;
            this.amount = amount;
        }

        public long getTimestamp() { return timestamp; }
        public String getLabel() { return label; }
        public double getAmount() { return amount; }
    }
}
