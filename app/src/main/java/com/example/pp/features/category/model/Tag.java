package com.example.pp.features.category.model;

public class Tag {
    private String id;
    private String name;
    private boolean selected;
    private Long validFrom;
    private Long validTill;

    public Tag(String id, String name) {
        this.id = id;
        this.name = name;
        this.selected = false;
    }

    public Tag(String id, String name, Long validFrom, Long validTill) {
        this.id = id;
        this.name = name;
        this.validFrom = validFrom;
        this.validTill = validTill;
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

    public Long getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Long validFrom) {
        this.validFrom = validFrom;
    }

    public Long getValidTill() {
        return validTill;
    }

    public void setValidTill(Long validTill) {
        this.validTill = validTill;
    }
}
