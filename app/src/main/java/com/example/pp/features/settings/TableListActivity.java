package com.example.pp.features.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;

import java.util.ArrayList;
import java.util.List;

public class TableListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_list);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvTables = findViewById(R.id.rvTables);
        rvTables.setLayoutManager(new LinearLayoutManager(this));

        List<TableInfo> tables = new ArrayList<>();
        tables.add(new TableInfo("categories", "Categories", "Hierarchy and icons"));
        tables.add(new TableInfo("tags", "Tags", "Labels and identifiers"));
        tables.add(new TableInfo("transactions", "Transactions", "Core transaction data"));
        tables.add(new TableInfo("transaction_tag_cross_ref", "Transaction Tags", "Many-to-many relationship"));
        tables.add(new TableInfo("background_job_log", "Background Job Logs", "Backup Logs"));
        tables.add(new TableInfo("task_definitions", "Task Definition", "Background Task"));
        tables.add(new TableInfo("task_states", "Task Status", "Task Current Status"));
        tables.add(new TableInfo("hydration_intakes", "Hydration", "Daily Water Intake"));
        tables.add(new TableInfo("nutrient_data", "Nutrient Data", "Food Item Nutritional Info"));
        tables.add(new TableInfo("nutrition_intake", "Nutrients Intake", "Daily Calorie Intake"));
        tables.add(new TableInfo("screen_time", "Screen Time", "Daily Screen Time Monitoring"));
        tables.add(new TableInfo("sleep", "Sleep", "Daily Sleep Monitoring"));
        tables.add(new TableInfo("history_api_track", "History Tracking", "History API Tracking"));
        tables.add(new TableInfo("quotes_api_track", "Quotes Tracking", "Quotes API Tracking"));
        tables.add(new TableInfo("saved_quotes", "Quotes Saved", "Saved Quotes"));

        rvTables.setAdapter(new TableAdapter(tables));
    }

    private static class TableInfo {
        String dbName;
        String displayName;
        String subtitle;

        TableInfo(String dbName, String displayName, String subtitle) {
            this.dbName = dbName;
            this.displayName = displayName;
            this.subtitle = subtitle;
        }
    }

    private class TableAdapter extends RecyclerView.Adapter<TableAdapter.ViewHolder> {
        private final List<TableInfo> tables;

        TableAdapter(List<TableInfo> tables) {
            this.tables = tables;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_table, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            TableInfo table = tables.get(position);
            holder.tvName.setText(table.displayName);
            holder.tvSubtitle.setText(table.subtitle);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(TableListActivity.this, TableViewActivity.class);
                intent.putExtra("table_name", table.dbName);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return tables.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            TextView tvSubtitle;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvTableName);
                tvSubtitle = itemView.findViewById(R.id.tvTableSubtitle);
            }
        }
    }
}