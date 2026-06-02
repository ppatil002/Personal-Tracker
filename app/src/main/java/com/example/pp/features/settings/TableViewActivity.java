package com.example.pp.features.settings;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.PPApplication;
import com.example.pp.R;
import com.example.pp.core.enums.TableAlignment;
import com.example.pp.core.enums.TableColumnType;
import com.example.pp.data.repository.GenericTableRepository;
import com.example.pp.ui.components.table.adapter.GenericTableAdapter;
import com.example.pp.ui.components.table.model.TableColumn;
import com.example.pp.ui.components.table.model.TableConfig;
import com.example.pp.ui.components.table.model.TableQuery;
import com.example.pp.ui.components.table.model.TableRow;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class TableViewActivity extends AppCompatActivity {

    private String tableName;
    private LinearLayout headerContainer;
    private TextView tvPageInfo;
    private ProgressBar progressBar;
    
    private TableQuery currentQuery;
    private GenericTableAdapter adapter;
    private List<TableColumn> columns = new ArrayList<>();
    private final int columnWidth = 400; // Pixels
    
    private View btnPrev, btnNext, btnDeleteAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_view);

        tableName = getIntent().getStringExtra("table_name");
        if (tableName == null) {
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(tableName);

        headerContainer = findViewById(R.id.headerContainer);
        RecyclerView rvData = findViewById(R.id.rvData);
        tvPageInfo = findViewById(R.id.tvPageInfo);
        progressBar = findViewById(R.id.progressBar);
        btnDeleteAll = findViewById(R.id.btnDeleteAll);

        currentQuery = new TableQuery();
        currentQuery.setLimit(20);
        currentQuery.setOffset(0);

        rvData.setLayoutManager(new LinearLayoutManager(this));
        TableConfig config = new TableConfig();
        config.setEditableMode(false);
        adapter = new GenericTableAdapter(config);
        adapter.setFixedCellWidth(columnWidth);
        rvData.setAdapter(adapter);

        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        btnPrev.setOnClickListener(v -> {
            if (currentQuery.getOffset() > 0) {
                currentQuery.setOffset(currentQuery.getOffset() - currentQuery.getLimit());
                loadData();
            }
        });

        btnNext.setOnClickListener(v -> {
            currentQuery.setOffset(currentQuery.getOffset() + currentQuery.getLimit());
            loadData();
        });

        btnDeleteAll.setOnClickListener(v -> showTruncateConfirmation());

        loadData();
    }

    private void showTruncateConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Truncate Table")
                .setMessage("Are you sure you want to delete ALL records from " + tableName + "? This cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> {
                    truncateTable();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void truncateTable() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        PPApplication app = (PPApplication) getApplication();
        app.getGenericTableRepository().truncateTable(tableName, () -> {
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Table truncated successfully", Toast.LENGTH_SHORT).show();
            currentQuery.setOffset(0);
            loadData();
        }, new GenericTableRepository.TableDataCallback() {
            @Override public void onDataLoaded(List<String> columnNames, List<TableRow> rows, int totalRecords) {}
            @Override
            public void onError(String message) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(TableViewActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadData() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        PPApplication app = (PPApplication) getApplication();
        GenericTableRepository repository = app.getGenericTableRepository();

        repository.getRawTableData(tableName, currentQuery, new GenericTableRepository.TableDataCallback() {
            @Override
            public void onDataLoaded(List<String> columnNames, List<TableRow> rows, int totalRecords) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                
                // Build Column Models if not yet built
                if (columns.isEmpty()) {
                    for (String name : columnNames) {
                        TableColumn col = new TableColumn(name, name, TableColumnType.TEXT);
                        col.setWidthWeight(1.0f);
                        col.setAlignment(TableAlignment.CENTER);
                        columns.add(col);
                    }
                    adapter.setColumns(columns);
                    setupHeader(columnNames);
                }

                adapter.setRows(rows);
                
                int page = (currentQuery.getOffset() / currentQuery.getLimit()) + 1;
                int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / currentQuery.getLimit()));
                tvPageInfo.setText(String.format(java.util.Locale.getDefault(), "Page %d/%d | Rows: %d", page, totalPages, totalRecords));

                boolean hasPrev = page > 1;
                boolean hasNext = page < totalPages;

                btnPrev.setEnabled(hasPrev);
                btnNext.setEnabled(hasNext);
                btnPrev.setAlpha(hasPrev ? 1.0f : 0.4f);
                btnNext.setAlpha(hasNext ? 1.0f : 0.4f);
            }

            @Override
            public void onError(String message) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(TableViewActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupHeader(List<String> columnNames) {
        headerContainer.removeAllViews();
        for (String col : columnNames) {
            TextView tv = new TextView(this);
            tv.setText(col);
            tv.setTextColor(ContextCompat.getColor(this, R.color.accent_purple));
            tv.setTextSize(14);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setPadding(24, 16, 24, 16);
            tv.setGravity(Gravity.CENTER);
            
            // Fixed width for horizontal scrolling
            tv.setLayoutParams(new LinearLayout.LayoutParams(columnWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            
            // Add sorting click listener
            tv.setOnClickListener(v -> {
                if (col.equals(currentQuery.getSortBy())) {
                    String nextDir = currentQuery.getSortDirection().equals("ASC") ? "DESC" : "ASC";
                    currentQuery.setSortDirection(nextDir);
                } else {
                    currentQuery.setSortBy(col);
                    currentQuery.setSortDirection("ASC");
                }
                loadData();
            });

            headerContainer.addView(tv);
        }
    }
}