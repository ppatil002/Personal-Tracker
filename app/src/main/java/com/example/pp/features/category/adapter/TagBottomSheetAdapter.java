package com.example.pp.features.category.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.pp.R;
import com.example.pp.features.category.model.Tag;
import java.util.ArrayList;
import java.util.List;

public class TagBottomSheetAdapter extends RecyclerView.Adapter<TagBottomSheetAdapter.ViewHolder> {

    private final List<Tag> allTags;
    private final List<Tag> displayList;

    public TagBottomSheetAdapter(List<Tag> tags) {
        this.allTags = new ArrayList<>(tags);
        this.displayList = new ArrayList<>(tags);
    }

    public void filter(String query) {
        String lower = query.toLowerCase().trim();
        displayList.clear();
        for (Tag tag : allTags) {
            if (lower.isEmpty() || tag.getName().toLowerCase().contains(lower)) {
                displayList.add(tag);
            }
        }
        notifyDataSetChanged();
    }

    public List<Tag> getSelectedTags() {
        List<Tag> selected = new ArrayList<>();
        for (Tag tag : allTags) {
            if (tag.isSelected()) selected.add(tag);
        }
        return selected;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tag_bottom_sheet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Tag tag = displayList.get(position);
        holder.bind(tag);
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbTag;
        TextView tvTagName;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbTag = itemView.findViewById(R.id.cbTag);
            tvTagName = itemView.findViewById(R.id.tvTagName);
        }

        void bind(Tag tag) {
            tvTagName.setText(tag.getName());
            cbTag.setOnCheckedChangeListener(null);
            cbTag.setChecked(tag.isSelected());
            cbTag.setOnCheckedChangeListener((buttonView, isChecked) -> tag.setSelected(isChecked));
            itemView.setOnClickListener(v -> cbTag.setChecked(!cbTag.isChecked()));
        }
    }
}
