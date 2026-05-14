package com.example.pp.features.category.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pp.R;
import com.example.pp.data.local.entity.CategoryEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CategoryManagerAdapter extends RecyclerView.Adapter<CategoryManagerAdapter.ViewHolder> {

    private List<CategoryEntity> categories = new ArrayList<>();
    private Map<String, String> parentNames = new HashMap<>();
    private OnCategoryActionListener listener;

    public interface OnCategoryActionListener {
        void onEdit(CategoryEntity category);
        void onDelete(CategoryEntity category);
    }

    public void setOnCategoryActionListener(OnCategoryActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CategoryEntity> list) {
        this.categories = list;
        parentNames.clear();
        for (CategoryEntity cat : list) {
            if (cat.getParentCategoryId() == null) {
                parentNames.put(cat.getCategoryId(), "Root");
            }
        }
        for (CategoryEntity cat : list) {
            if (cat.getParentCategoryId() != null) {
                String pName = "Unknown";
                for (CategoryEntity p : list) {
                    if (p.getCategoryId().equals(cat.getParentCategoryId())) {
                        pName = p.getName();
                        break;
                    }
                }
                parentNames.put(cat.getCategoryId(), pName);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_manager, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryEntity category = categories.get(position);
        holder.tvName.setText(category.getName());
        holder.tvIcon.setText(category.getIcon());
        String pName = parentNames.get(category.getCategoryId());
        holder.tvParent.setText(pName != null ? "Parent: " + pName : "");

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(category);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvParent;
        ImageView btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvParent = itemView.findViewById(R.id.tvParentName);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
