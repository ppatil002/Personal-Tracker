package com.example.pp.features.analytics.model;

public class CategoryExpense {
    private String categoryId;
    private String parentCategoryId;
    private String name;
    private String icon;
    private String colour;
    private double amount;
    private double percentage;

    public CategoryExpense(String categoryId, String parentCategoryId, String name, String icon, String colour, double amount, double percentage) {
        this.categoryId = categoryId;
        this.parentCategoryId = parentCategoryId;
        this.name = name;
        this.icon = icon;
        this.colour = colour;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getCategoryId() { return categoryId; }
    public String getParentCategoryId() { return parentCategoryId; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public String getColour() { return colour; }
    public double getAmount() { return amount; }
    public double getPercentage() { return percentage; }

    public void setAmount(double amount) { this.amount = amount; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}
