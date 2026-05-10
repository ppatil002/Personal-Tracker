package com.example.pp.ui.components.table.model;

public class TableFilter {
    private String field;
    private Operator operator;
    private Object value;

    public enum Operator {
        EQUALS, CONTAINS, GREATER_THAN, LESS_THAN, BETWEEN
    }

    public TableFilter(String field, Operator operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    // Getters and Setters
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}