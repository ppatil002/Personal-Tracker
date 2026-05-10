package com.example.pp.domain.model;

import com.example.pp.data.local.entity.CategoryEntity;
import java.util.ArrayList;
import java.util.List;

public class CategoryTreeNode {
    private final CategoryEntity category;
    private final List<CategoryTreeNode> children;
    private boolean isExpanded;
    private int level;

    public CategoryTreeNode(CategoryEntity category) {
        this.category = category;
        this.children = new ArrayList<>();
        this.isExpanded = false;
        this.level = 1;
    }

    public CategoryEntity getCategory() { return category; }
    public List<CategoryTreeNode> getChildren() { return children; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public void addChild(CategoryTreeNode child) {
        child.setLevel(this.level + 1);
        this.children.add(child);
    }
    
    public String getName() { return category.getName(); }
    public String getIcon() { return category.getIcon(); } // This is now the Emoji string
    public String getCategoryId() { return category.getCategoryId(); }
    public String getColour() { return category.getColour(); }
}