package com.example.pp.features.analytics.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.features.analytics.model.CategoryExpense;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryExpenseAdapter extends RecyclerView.Adapter<CategoryExpenseAdapter.ViewHolder> {

    private final List<CategoryExpense> items = new ArrayList<>();
    private final int layoutResId;
    private OnItemClickListener listener;

    public CategoryExpenseAdapter() {
        this(R.layout.item_analytics_category);
    }

    public CategoryExpenseAdapter(int layoutResId) {
        this.layoutResId = layoutResId;
    }

    public interface OnItemClickListener {
        void onItemClick(CategoryExpense item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CategoryExpense> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryExpense item = items.get(position);
        if (holder.tvIcon != null) {
            holder.tvIcon.setText(item.getIcon() != null ? item.getIcon() : "\u2022");
            try {
                holder.tvIcon.setTextColor(Color.parseColor(item.getColour()));
            } catch (Exception e) {
                holder.tvIcon.setTextColor(Color.WHITE);
            }
        }
        if (holder.tvName != null) holder.tvName.setText(item.getName());
        if (holder.tvAmount != null) {
            holder.tvAmount.setText(String.format(Locale.getDefault(), "\u20B9 %,.0f", item.getAmount()));
        }
        if (holder.tvPercent != null) {
            holder.tvPercent.setText(String.format(Locale.getDefault(), "%.0f%%", item.getPercentage()));
        }
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvAmount, tvPercent;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvAmount = itemView.findViewById(R.id.tvCategoryAmount);
            tvPercent = itemView.findViewById(R.id.tvCategoryPercent);
        }
    }
}
