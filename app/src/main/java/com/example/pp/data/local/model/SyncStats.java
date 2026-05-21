package com.example.pp.data.local.model;

import androidx.annotation.NonNull;

import java.util.Locale;

public class SyncStats {
    public int inserted = 0;
    public int updated = 0;
    public int previousCount = 0;
    public int finalCount = 0;
    public String tableName;

    public SyncStats(String tableName) {
        this.tableName = tableName;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%s: Prev: %d, Ins: %d, Upd: %d, Final: %d", 
            tableName, previousCount, inserted, updated, finalCount);
    }
}
