package com.example.pp.ui.components.table.model;

public class TableConfig {
    private boolean editableMode;
    private boolean selectableRows;
    private boolean searchable;
    private boolean sortable;
    private boolean paginationEnabled;
    private boolean showHeaders;

    public TableConfig() {
        this.editableMode = false;
        this.selectableRows = false;
        this.searchable = true;
        this.sortable = true;
        this.paginationEnabled = true;
        this.showHeaders = true;
    }

    // Getters and Setters
    public boolean isEditableMode() { return editableMode; }
    public void setEditableMode(boolean editableMode) { this.editableMode = editableMode; }

    public boolean isSelectableRows() { return selectableRows; }
    public void setSelectableRows(boolean selectableRows) { this.selectableRows = selectableRows; }

    public boolean isSearchable() { return searchable; }
    public void setSearchable(boolean searchable) { this.searchable = searchable; }

    public boolean isSortable() { return sortable; }
    public void setSortable(boolean sortable) { this.sortable = sortable; }

    public boolean isPaginationEnabled() { return paginationEnabled; }
    public void setPaginationEnabled(boolean paginationEnabled) { this.paginationEnabled = paginationEnabled; }

    public boolean isShowHeaders() { return showHeaders; }
    public void setShowHeaders(boolean showHeaders) { this.showHeaders = showHeaders; }
}