package com.example.pp.ui.components.table.model;

import com.example.pp.core.enums.TableFilterOperator;

public class TableFilter {
    private String field;
    private TableFilterOperator operator;
    private Object value;

    public TableFilter(String field, TableFilterOperator operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    // Getters and Setters
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public TableFilterOperator getOperator() { return operator; }
    public void setOperator(TableFilterOperator operator) { this.operator = operator; }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
