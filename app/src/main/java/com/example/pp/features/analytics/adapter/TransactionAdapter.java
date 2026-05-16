package com.example.pp.features.analytics.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.data.local.entity.TransactionEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionEntity> transactions = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public void submitList(List<TransactionEntity> list) {
        this.transactions = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // We can reuse a simple layout or use item_tracker if it fits
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_analytics_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionEntity t = transactions.get(position);
        holder.tvName.setText(t.getItem());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₹ %.0f", t.getAmount()));
        holder.tvIcon.setText("💰"); // Default icon
        holder.tvPercent.setText(timeFormat.format(t.getTransactionDate()));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvAmount, tvPercent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvAmount = itemView.findViewById(R.id.tvCategoryAmount);
            tvPercent = itemView.findViewById(R.id.tvCategoryPercent);
        }
    }
}
