package com.example.pp.ui.components.table.model;

import com.example.pp.core.enums.TableColumnType;

public class TableCell {
    private String columnKey;
    private String value;
    private Object rawValue;
    private Integer color;
    private String emoji;
    private boolean editable;
    private TableColumnType cellType;
    private int colSpan = 1;
    private Integer backgroundColor;

    public TableCell(String columnKey, String value, Object rawValue, TableColumnType cellType) {
        this.columnKey = columnKey;
        this.value = value;
        this.rawValue = rawValue;
        this.cellType = cellType;
        this.editable = false;
        this.colSpan = 1;
    }

    public TableCell(String columnKey, String value, Object rawValue, TableColumnType cellType, int colSpan) {
        this(columnKey, value, rawValue, cellType);
        this.colSpan = colSpan;
    }

    // Getters and Setters
    public String getColumnKey() { return columnKey; }
    public void setColumnKey(String columnKey) { this.columnKey = columnKey; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Object getRawValue() { return rawValue; }
    public void setRawValue(Object rawValue) { this.rawValue = rawValue; }

    public Integer getColor() { return color; }
    public void setColor(Integer color) { this.color = color; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }

    public TableColumnType getCellType() { return cellType; }
    public void setCellType(TableColumnType cellType) { this.cellType = cellType; }

    public int getColSpan() { return colSpan; }
    public void setColSpan(int colSpan) { this.colSpan = colSpan; }

    public Integer getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(Integer backgroundColor) { this.backgroundColor = backgroundColor; }
}