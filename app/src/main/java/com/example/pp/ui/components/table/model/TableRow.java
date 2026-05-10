package com.example.pp.ui.components.table.model;

import java.util.List;

public class TableRow {
    private String rowId;
    private List<TableCell> cells;
    private Object originalEntity;
    private boolean editable;

    public TableRow(String rowId, List<TableCell> cells, Object originalEntity) {
        this.rowId = rowId;
        this.cells = cells;
        this.originalEntity = originalEntity;
        this.editable = false;
    }

    // Getters and Setters
    public String getRowId() { return rowId; }
    public void setRowId(String rowId) { this.rowId = rowId; }

    public List<TableCell> getCells() { return cells; }
    public void setCells(List<TableCell> cells) { this.cells = cells; }

    public Object getOriginalEntity() { return originalEntity; }
    public void setOriginalEntity(Object originalEntity) { this.originalEntity = originalEntity; }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
}