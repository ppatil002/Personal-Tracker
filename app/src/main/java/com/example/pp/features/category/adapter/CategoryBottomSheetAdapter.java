package com.example.pp.features.category.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.domain.model.CategoryTreeNode;

import java.util.ArrayList;
import java.util.List;

public class CategoryBottomSheetAdapter extends RecyclerView.Adapter<CategoryBottomSheetAdapter.ViewHolder> {

    private final List<CategoryTreeNode> rootCategories;
    private final List<CategoryTreeNode> displayList;
    private OnCategorySelectedListener listener;
    private String currentFilter = "";

    public interface OnCategorySelectedListener {
        void onCategorySelected(CategoryTreeNode category);
    }

    public CategoryBottomSheetAdapter(List<CategoryTreeNode> categories) {
        this.rootCategories = categories;
        this.displayList = new ArrayList<>();
        generateDisplayList();
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    public void filter(String query) {
        this.currentFilter = query.toLowerCase().trim();
        generateDisplayList();
        notifyDataSetChanged();
    }

    private void generateDisplayList() {
        displayList.clear();
        for (CategoryTreeNode root : rootCategories) {
            addCategoryToDisplayList(root);
        }
    }

    private void addCategoryToDisplayList(CategoryTreeNode node) {
        if (currentFilter.isEmpty()) {
            displayList.add(node);
            if (node.isExpanded()) {
                for (CategoryTreeNode child : node.getChildren()) {
                    addCategoryToDisplayList(child);
                }
            }
        } else {
            if (node.getName().toLowerCase().contains(currentFilter)) {
                displayList.add(node);
            }
            for (CategoryTreeNode child : node.getChildren()) {
                addCategoryToDisplayList(child);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_bottom_sheet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryTreeNode node = displayList.get(position);
        holder.bind(node);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvIcon;
        ImageView ivAction;
        View rootLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rootLayout = itemView.findViewById(R.id.rootLayout);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
            ivAction = itemView.findViewById(R.id.ivAction);
        }

        void bind(CategoryTreeNode node) {
            tvName.setText(node.getName());
            tvIcon.setText(node.getIcon());

            int indent = (node.getLevel() - 1) * 36;
            rootLayout.setPadding(indent + 16, rootLayout.getPaddingTop(), rootLayout.getPaddingEnd(), rootLayout.getPaddingBottom());

            if (node.getChildren().isEmpty()) {
                ivAction.setVisibility(View.GONE);
                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onCategorySelected(node);
                });
            } else {
                ivAction.setVisibility(View.VISIBLE);
                ivAction.setImageResource(node.isExpanded() ? R.drawable.ic_collapse : R.drawable.ic_expand);
                ivAction.setOnClickListener(v -> {
                    node.setExpanded(!node.isExpanded());
                    generateDisplayList();
                    notifyDataSetChanged();
                });
                itemView.setOnClickListener(v -> {
                    node.setExpanded(!node.isExpanded());
                    generateDisplayList();
                    notifyDataSetChanged();
                });
            }
        }
    }
}