package com.example.pp.data.local.query;

import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.example.pp.ui.components.table.model.TableFilter;
import com.example.pp.ui.components.table.model.TableQuery;

import java.util.ArrayList;
import java.util.List;

public class TableQueryBuilder {

    public static SupportSQLiteQuery build(String tableName, TableQuery query) {
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName);
        List<Object> args = new ArrayList<>();

        boolean hasWhere = false;

        // Search text
        if (query.getSearchText() != null && !query.getSearchText().isEmpty()) {
            sql.append(" WHERE name LIKE ?"); // Assuming 'name' exists in the table, or use a generic approach
            args.add("%" + query.getSearchText() + "%");
            hasWhere = true;
        }

        // Filters
        if (query.getFilters() != null && !query.getFilters().isEmpty()) {
            for (TableFilter filter : query.getFilters()) {
                sql.append(hasWhere ? " AND " : " WHERE ");
                sql.append(filter.getField());
                
                switch (filter.getOperator()) {
                    case EQUALS: sql.append(" = ?"); args.add(filter.getValue()); break;
                    case CONTAINS: sql.append(" LIKE ?"); args.add("%" + filter.getValue() + "%"); break;
                    case GREATER_THAN: sql.append(" > ?"); args.add(filter.getValue()); break;
                    case LESS_THAN: sql.append(" < ?"); args.add(filter.getValue()); break;
                    case BETWEEN: 
                        sql.append(" BETWEEN ? AND ?"); 
                        Object[] vals = (Object[]) filter.getValue();
                        args.add(vals[0]);
                        args.add(vals[1]);
                        break;
                }
                hasWhere = true;
            }
        }

        // Sorting
        if (query.getSortBy() != null && !query.getSortBy().isEmpty()) {
            sql.append(" ORDER BY ").append(query.getSortBy()).append(" ").append(query.getSortDirection());
        }

        // Pagination
        sql.append(" LIMIT ? OFFSET ?");
        args.add(query.getLimit());
        args.add(query.getOffset());

        return new SimpleSQLiteQuery(sql.toString(), args.toArray());
    }
}