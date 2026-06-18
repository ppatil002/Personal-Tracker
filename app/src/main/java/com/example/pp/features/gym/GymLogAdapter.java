package com.example.pp.features.gym;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.data.local.entity.GymTracking;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class GymLogAdapter extends RecyclerView.Adapter<GymLogAdapter.ViewHolder> {
    private List<MuscleGroup> groups = new ArrayList<>();
    private OnLogClickListener listener;

    public interface OnLogClickListener {
        void onLogClick(GymTracking log);
    }

    public void setOnLogClickListener(OnLogClickListener listener) {
        this.listener = listener;
    }

    public void setLogs(List<GymTracking> logs) {
        this.groups = groupLogs(logs);
        notifyDataSetChanged();
    }

    private List<MuscleGroup> groupLogs(List<GymTracking> logs) {
        // Group by Muscle (TreeMap to keep order or alphabetical)
        Map<String, MuscleGroup> muscleMap = new TreeMap<>();
        for (GymTracking log : logs) {
            MuscleGroup mGroup = muscleMap.get(log.muscle);
            if (mGroup == null) {
                mGroup = new MuscleGroup(log.muscle, log.exerciseDate);
                muscleMap.put(log.muscle, mGroup);
            }
            
            // Under Muscle, group by Exercise
            ExerciseGroup eGroup = mGroup.exercises.get(log.exercise);
            if (eGroup == null) {
                eGroup = new ExerciseGroup(log.exercise);
                mGroup.exercises.put(log.exercise, eGroup);
            }
            eGroup.sets.add(log);
        }
        return new ArrayList<>(muscleMap.values());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gym_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MuscleGroup group = groups.get(position);
        holder.tvMuscle.setText(group.muscle);
        holder.containerLogs.removeAllViews();
        
        int exerciseCount = group.exercises.size();
        int currentExerciseIndex = 0;
        
        for (ExerciseGroup eGroup : group.exercises.values()) {
            LinearLayout exerciseLayout = new LinearLayout(holder.itemView.getContext());
            exerciseLayout.setOrientation(LinearLayout.VERTICAL);
            exerciseLayout.setPadding(0, 8, 0, 8);
            
            android.util.TypedValue outValue = new android.util.TypedValue();
            holder.itemView.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            exerciseLayout.setBackgroundResource(outValue.resourceId);
            
            exerciseLayout.setClickable(true);
            exerciseLayout.setFocusable(true);

            TextView tvTitle = new TextView(holder.itemView.getContext());
            tvTitle.setText(eGroup.exerciseName.toUpperCase());
            tvTitle.setTextColor(holder.tvMuscle.getCurrentTextColor());
            tvTitle.setTextSize(14);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            exerciseLayout.addView(tvTitle);

            for (GymTracking log : eGroup.sets) {
                TextView tvSet = new TextView(holder.itemView.getContext());
                String formatted = formatSets(log.setsJson);
                StringBuilder sb = new StringBuilder(formatted);
                if (log.difficulty != null && !log.difficulty.isEmpty()) {
                    sb.append(" (").append(log.difficulty).append(")");
                }
                tvSet.setText(sb.toString());
                tvSet.setTextColor(0xFFBBBBBB);
                tvSet.setTextSize(12);
                tvSet.setPadding(20, 4, 0, 0);
                exerciseLayout.addView(tvSet);

                exerciseLayout.setOnClickListener(v -> {
                    if (listener != null) listener.onLogClick(log);
                });
            }
            
            holder.containerLogs.addView(exerciseLayout);
            
            // Add a small divider if not last
            if (currentExerciseIndex < exerciseCount - 1) {
                View divider = new View(holder.itemView.getContext());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
                lp.setMargins(0, 4, 0, 4);
                divider.setLayoutParams(lp);
                divider.setBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.stroke_color));
                holder.containerLogs.addView(divider);
            }
            currentExerciseIndex++;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(group.date)));
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    private String formatSets(String json) {
        if (json == null || json.isEmpty()) return "";
        try {
            if (json.startsWith("[{")) {
                // New format: JSON Array of Objects
                List<Map<String, String>> sets = new Gson().fromJson(json, new TypeToken<List<Map<String, String>>>(){}.getType());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < sets.size(); i++) {
                    Map<String, String> set = sets.get(i);
                    String reps = set.get("reps");
                    String weight = set.get("weight");
                    String diff = set.get("diff");

                    if (reps != null && !reps.isEmpty()) {
                        sb.append(reps).append(" reps");
                        if (weight != null && !weight.isEmpty()) sb.append(" @ ").append(weight).append("kg");
                        if (diff != null && !diff.isEmpty()) sb.append(" [").append(diff).append("]");
                        if (i < sets.size() - 1) sb.append("\n"); // Use newline instead of comma
                    }
                }
                return sb.toString();
            }
        } catch (Exception e) {
            // Fallback to old format or raw string
        }
        return json.replace("{", "").replace("}", "").replace("[", "").replace("]", "").replace("\"", "");
    }

    static class MuscleGroup {
        String muscle;
        long date;
        // Exercise Name -> List of logs (sets)
        Map<String, ExerciseGroup> exercises = new LinkedHashMap<>();
        
        MuscleGroup(String muscle, long date) {
            this.muscle = muscle;
            this.date = date;
        }
    }

    static class ExerciseGroup {
        String exerciseName;
        List<GymTracking> sets = new ArrayList<>();
        
        ExerciseGroup(String exerciseName) {
            this.exerciseName = exerciseName;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMuscle, tvDate;
        LinearLayout containerLogs;

        ViewHolder(View view) {
            super(view);
            tvMuscle = view.findViewById(R.id.tvMuscle);
            tvDate = view.findViewById(R.id.tvDate);
            containerLogs = view.findViewById(R.id.containerLogs);
        }
    }
}
