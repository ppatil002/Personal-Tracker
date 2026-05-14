package com.example.pp.ui.components.table.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pp.R;
import com.example.pp.ui.components.table.model.TableColumn;
import com.example.pp.ui.components.table.model.TableCell;
import com.example.pp.ui.components.table.model.TableRow;
import com.example.pp.ui.components.table.model.TableConfig;

import java.util.ArrayList;
import java.util.List;

public class GenericTableAdapter extends RecyclerView.Adapter<GenericTableAdapter.RowViewHolder> {

    private List<TableColumn> columns = new ArrayList<>();
    private List<TableRow> rows = new ArrayList<>();
    private final TableConfig config;
    private OnRowClickListener rowClickListener;
    private int fixedCellWidth = -1; // 0 or negative means use weight

    public interface OnRowClickListener {
        void onRowClick(TableRow row);
    }

    public GenericTableAdapter(TableConfig config) {
        this.config = config;
    }

    public void setColumns(List<TableColumn> columns) {
        this.columns = columns;
        notifyDataSetChanged();
    }

    public void setRows(List<TableRow> rows) {
        this.rows = rows;
        notifyDataSetChanged();
    }

    public void setFixedCellWidth(int width) {
        this.fixedCellWidth = width;
    }

    public void setOnRowClickListener(OnRowClickListener listener) {
        this.rowClickListener = listener;
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_generic_table_row, parent, false);
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        TableRow row = rows.get(position);
        holder.bind(row, columns, config, rowClickListener, fixedCellWidth, position);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        LinearLayout rowContainer;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            rowContainer = itemView.findViewById(R.id.rowContainer);
        }

        void bind(TableRow row, List<TableColumn> columns, TableConfig config, OnRowClickListener listener, int fixedWidth, int position) {
            rowContainer.removeAllViews();
            
            boolean isFooter = row.getRowId().contains("footer");
            int skipColumns = 0;
            for (int i = 0; i < columns.size(); i++) {
                TableColumn column = columns.get(i);
                if (!column.isVisible()) continue;

                if (skipColumns > 0) {
                    skipColumns--;
                    continue;
                }

                TableCell cell = findCell(row, column.getKey());
                View cellView = renderCell(rowContainer, cell, column, config);
                
                if (isFooter && cellView instanceof TextView) {
                    ((TextView) cellView).setTypeface(null, android.graphics.Typeface.BOLD);
                }

                int span = (cell != null) ? cell.getColSpan() : 1;
                if (span > 1) {
                    skipColumns = span - 1;
                }

                LinearLayout.LayoutParams params;
                if (fixedWidth > 0) {
                    params = new LinearLayout.LayoutParams(fixedWidth * span, ViewGroup.LayoutParams.WRAP_CONTENT);
                } else {
                    params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, column.getWidthWeight() * span);
                }
                cellView.setLayoutParams(params);
                rowContainer.addView(cellView);
            }

            if (isFooter) {
                rowContainer.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.table_footer_bg));
            } else if (position % 2 == 0) {
                rowContainer.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.table_row_even));
            } else {
                rowContainer.setBackgroundColor(Color.TRANSPARENT);
            }

            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onRowClick(row));
                itemView.setClickable(!config.isEditableMode());
            }
        }

        private TableCell findCell(TableRow row, String key) {
            for (TableCell cell : row.getCells()) {
                if (cell.getColumnKey().equals(key)) return cell;
            }
            return null;
        }

        private View renderCell(ViewGroup parent, TableCell cell, TableColumn column, TableConfig config) {
            TextView textView = new TextView(parent.getContext());
            textView.setPadding(24, 16, 24, 16);
            textView.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.text_primary));
            textView.setTextSize(13);
            
            if (cell != null) {
                String displayValue = cell.getValue();
                if (cell.getEmoji() != null) {
                    displayValue = cell.getEmoji() + " " + displayValue;
                }
                textView.setText(displayValue);
                if (cell.getColor() != null) {
                    textView.setTextColor(cell.getColor());
                }
                
                if (cell.getColSpan() > 1) {
                    textView.setGravity(android.view.Gravity.CENTER);
                }
            } else {
                textView.setText("-");
            }

            if (cell == null || cell.getColSpan() <= 1) {
                switch (column.getAlignment()) {
                    case CENTER: textView.setGravity(android.view.Gravity.CENTER); break;
                    case RIGHT: textView.setGravity(android.view.Gravity.END); break;
                    default: textView.setGravity(android.view.Gravity.START); break;
                }
            }

            return textView;
        }
    }
}