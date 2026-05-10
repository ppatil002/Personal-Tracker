package com.example.pp.features.category.model;

public class Tag {
    private String id;
    private String name;
    private boolean selected;

    public Tag(String id, String name) {
        this.id = id;
        this.name = name;
        this.selected = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
