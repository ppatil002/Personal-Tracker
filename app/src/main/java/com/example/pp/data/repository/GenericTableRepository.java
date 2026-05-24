package com.example.pp.data.repository;

import android.content.Context;
import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.pp.core.enums.TableColumnType;
import com.example.pp.core.task.AppExecutors;
import com.example.pp.data.local.AppDatabase;
import com.example.pp.data.local.dao.CommonDao;
import com.example.pp.data.local.query.TableQueryBuilder;
import com.example.pp.ui.components.table.model.TableCell;
import com.example.pp.ui.components.table.model.TableQuery;
import com.example.pp.ui.components.table.model.TableRow;

import java.util.ArrayList;
import java.util.List;

public class GenericTableRepository {
    private final CommonDao commonDao;
    private final AppExecutors executors;

    public GenericTableRepository(Context context) {
        this.commonDao = AppDatabase.getInstance(context).commonDao();
        this.executors = AppExecutors.getInstance();
    }

    public interface TableDataCallback {
        void onDataLoaded(List<String> columnNames, List<TableRow> rows);
        void onError(String message);
    }

    public void getRawTableData(String tableName, TableQuery query, TableDataCallback callback) {
        executors.diskIO().execute(() -> {
            try {
                SupportSQLiteQuery sqliteQuery = TableQueryBuilder.build(tableName, query);
                Cursor cursor = commonDao.getRawTableData(sqliteQuery);

                if (cursor != null) {
                    List<String> columnNames = new ArrayList<>();
                    String[] colArray = cursor.getColumnNames();
                    for (String col : colArray) {
                        columnNames.add(col);
                    }

                    List<TableRow> rows = new ArrayList<>();
                    int rowIdCount = 0;
                    while (cursor.moveToNext()) {
                        List<TableCell> cells = new ArrayList<>();
                        for (int i = 0; i < colArray.length; i++) {
                            String val = cursor.getString(i);
                            if (val == null) val = "NULL";
                            
                            // Map to generic TableCell
                            cells.add(new TableCell(colArray[i], val, val, TableColumnType.TEXT));
                        }
                        rows.add(new TableRow(String.valueOf(rowIdCount++), cells, null));
                    }
                    cursor.close();

                    executors.mainThread().execute(() -> callback.onDataLoaded(columnNames, rows));
                } else {
                    executors.mainThread().execute(() -> callback.onError("Null cursor returned"));
                }
            } catch (Exception e) {
                executors.mainThread().execute(() -> callback.onError(e.getMessage()));
            }
        });
    }
}