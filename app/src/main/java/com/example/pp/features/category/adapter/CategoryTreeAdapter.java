package com.example.pp.features.category.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pp.R;
import com.example.pp.features.category.model.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryTreeAdapter extends RecyclerView.Adapter<CategoryTreeAdapter.ViewHolder> {

    private List<Category> allCategories; // Roots
    private List<Category> displayList;
    private OnCategorySelectedListener listener;

    public interface OnCategorySelectedListener {
        void onCategorySelected(Category category);
    }

    public CategoryTreeAdapter(List<Category> categories) {
        this.allCategories = categories;
        this.displayList = new ArrayList<>();
        generateDisplayList();
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    private void generateDisplayList() {
        displayList.clear();
        for (Category root : allCategories) {
            addCategoryToDisplayList(root);
        }
    }

    private void addCategoryToDisplayList(Category category) {
        displayList.add(category);
        if (category.isExpanded()) {
            for (Category child : category.getChildren()) {
                addCategoryToDisplayList(child);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_tree, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = displayList.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivIcon;
        ImageView btnAction;
        View viewConnector;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
            btnAction = itemView.findViewById(R.id.btnAction);
            viewConnector = itemView.findViewById(R.id.viewConnector);
        }

        public void bind(Category category) {
            tvName.setText(category.getName());
            // In a real app, you'd set the icon from category.getIcon()
            // ivIcon.setImageResource(category.getIcon());
            
            // Indentation based on level
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
            int marginStart = (category.getLevel() - 1) * 48; // 48dp per level
            itemView.setPadding(marginStart, itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getPaddingBottom());

            if (category.getChildren().isEmpty()) {
                btnAction.setImageResource(android.R.drawable.ic_input_add); // Plus icon for L3
                btnAction.setOnClickListener(v -> {
                    if (listener != null) listener.onCategorySelected(category);
                });
            } else {
                btnAction.setImageResource(category.isExpanded() ? android.R.drawable.arrow_up_float : android.R.drawable.arrow_down_float);
                btnAction.setOnClickListener(v -> {
                    category.setExpanded(!category.isExpanded());
                    generateDisplayList();
                    notifyDataSetChanged();
                });
            }
            
            itemView.setOnClickListener(v -> {
                if (category.getChildren().isEmpty()) {
                   if (listener != null) listener.onCategorySelected(category);
                } else {
                    category.setExpanded(!category.isExpanded());
                    generateDisplayList();
                    notifyDataSetChanged();
                }
            });
        }
    }
}