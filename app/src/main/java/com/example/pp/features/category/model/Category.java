package com.example.pp.features.category.model;

import java.util.ArrayList;
import java.util.List;

public class Category {
    private String id;
    private String parentCategoryId;
    private String name;
    private int icon;
    private String colour;
    private List<Category> children;
    private boolean isExpanded = false;
    private int level = 1; // L1, L2, L3

    public Category(String id, String name, int icon, String parentCategoryId) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.parentCategoryId = parentCategoryId;
        this.children = new ArrayList<>();
    }

    public String getId() { return id; }
    public String getParentCategoryId() { return parentCategoryId; }
    public String getName() { return name; }
    public int getIcon() { return icon; }
    public String getColour() { return colour; }
    public List<Category> getChildren() { return children; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public void addChild(Category child) {
        child.setLevel(this.level + 1);
        this.children.add(child);
    }
}