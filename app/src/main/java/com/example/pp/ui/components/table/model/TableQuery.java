package com.example.pp.ui.components.table.model;

import java.util.ArrayList;
import java.util.List;

public class TableQuery {
    private String searchText;
    private String sortBy;
    private String sortDirection; // ASC, DESC
    private List<TableFilter> filters;
    private int limit;
    private int offset;
    private boolean editableMode;

    public TableQuery() {
        this.filters = new ArrayList<>();
        this.limit = 20;
        this.offset = 0;
        this.sortDirection = "ASC";
    }

    // Getters and Setters
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

    public List<TableFilter> getFilters() { return filters; }
    public void setFilters(List<TableFilter> filters) { this.filters = filters; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public boolean isEditableMode() { return editableMode; }
    public void setEditableMode(boolean editableMode) { this.editableMode = editableMode; }
}