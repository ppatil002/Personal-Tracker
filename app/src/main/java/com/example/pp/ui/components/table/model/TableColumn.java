package com.example.pp.ui.components.table.model;

import com.example.pp.core.enums.TableAlignment;
import com.example.pp.core.enums.TableColumnType;

public class TableColumn {
    private String key;
    private String title;
    private TableColumnType type;
    private boolean sortable;
    private boolean editable;
    private boolean visible;
    private float widthWeight;
    private TableAlignment alignment;

    public TableColumn(String key, String title, TableColumnType type) {
        this.key = key;
        this.title = title;
        this.type = type;
        this.sortable = true;
        this.editable = false;
        this.visible = true;
        this.widthWeight = 1.0f;
        this.alignment = TableAlignment.LEFT;
    }

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public TableColumnType getType() { return type; }
    public void setType(TableColumnType type) { this.type = type; }

    public boolean isSortable() { return sortable; }
    public void setSortable(boolean sortable) { this.sortable = sortable; }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public float getWidthWeight() { return widthWeight; }
    public void setWidthWeight(float widthWeight) { this.widthWeight = widthWeight; }

    public TableAlignment getAlignment() { return alignment; }
    public void setAlignment(TableAlignment alignment) { this.alignment = alignment; }
}
