package com.example.pp.data.local.dao;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

@Dao
public interface CommonDao {
    @RawQuery
    Cursor getRawTableData(SupportSQLiteQuery query);

    @RawQuery
    int executeRawQuery(SupportSQLiteQuery query);
}