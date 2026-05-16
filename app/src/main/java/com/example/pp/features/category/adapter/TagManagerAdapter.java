package com.example.pp.features.category.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.data.local.entity.TagEntity;

import java.util.ArrayList;
import java.util.List;

public class TagManagerAdapter extends RecyclerView.Adapter<TagManagerAdapter.ViewHolder> {

    private List<TagEntity> tags = new ArrayList<>();
    private OnTagActionListener listener;

    public interface OnTagActionListener {
        void onEdit(TagEntity tag);
        void onDelete(TagEntity tag);
    }

    public void setOnTagActionListener(OnTagActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<TagEntity> list) {
        this.tags = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tag_manager, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TagEntity tag = tags.get(position);
        holder.tvName.setText(tag.getName());
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(tag);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(tag);
        });
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTagName);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
