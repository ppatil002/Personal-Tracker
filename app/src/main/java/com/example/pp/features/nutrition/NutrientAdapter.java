package com.example.pp.features.nutrition;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.data.local.entity.NutrientEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NutrientAdapter extends RecyclerView.Adapter<NutrientAdapter.NutrientViewHolder> {

    private List<NutrientEntity> nutrients = new ArrayList<>();
    private final OnNutrientClickListener listener;

    public interface OnNutrientClickListener {
        void onEdit(NutrientEntity nutrient);
        void onDelete(NutrientEntity nutrient);
    }

    public NutrientAdapter(OnNutrientClickListener listener) {
        this.listener = listener;
    }

    public void setNutrients(List<NutrientEntity> nutrients) {
        this.nutrients = nutrients;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NutrientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_nutrient, parent, false);
        return new NutrientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NutrientViewHolder holder, int position) {
        NutrientEntity nutrient = nutrients.get(position);
        holder.tvFoodItem.setText(nutrient.getFoodItem());
        holder.tvNutrientDetails.setText(String.format(Locale.getDefault(), "Per %.0f %s", nutrient.getPerUnit(), nutrient.getUnit()));
        
        String stats = String.format(Locale.getDefault(), 
            "Cals: %.1f | P: %.1fg | C: %.1fg | F: %.1fg", 
            nutrient.getCalories(), nutrient.getProtein(), nutrient.getCarbs(), nutrient.getFats());
        holder.tvStats.setText(stats);

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(nutrient));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(nutrient));
    }

    @Override
    public int getItemCount() {
        return nutrients.size();
    }

    static class NutrientViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodItem, tvNutrientDetails, tvStats;
        ImageView btnEdit, btnDelete;

        public NutrientViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodItem = itemView.findViewById(R.id.tvFoodItem);
            tvNutrientDetails = itemView.findViewById(R.id.tvNutrientDetails);
            tvStats = itemView.findViewById(R.id.tvStats);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
