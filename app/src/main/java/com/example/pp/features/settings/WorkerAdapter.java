package com.example.pp.features.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;
import com.example.pp.data.local.dao.TaskDao;
import com.example.pp.data.local.entity.TaskDefinition;
import com.example.pp.data.local.entity.TaskState;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.WorkerViewHolder> {

    private List<TaskDao.TaskWithState> tasks = new ArrayList<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

    public void setTasks(List<TaskDao.TaskWithState> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_worker, parent, false);
        return new WorkerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkerViewHolder holder, int position) {
        holder.bind(tasks.get(position));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class WorkerViewHolder extends RecyclerView.ViewHolder {
        TextView tvTaskType, tvStatus, tvTaskId, tvLastRun, tvNextRun, tvInterval, tvConstraints, tvProgress, tvFailureReason;
        ProgressBar pbProgress;
        View layoutFailure;

        WorkerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTaskType = itemView.findViewById(R.id.tvTaskType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvTaskId = itemView.findViewById(R.id.tvTaskId);
            tvLastRun = itemView.findViewById(R.id.tvLastRun);
            tvNextRun = itemView.findViewById(R.id.tvNextRun);
            tvInterval = itemView.findViewById(R.id.tvInterval);
            tvConstraints = itemView.findViewById(R.id.tvConstraints);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            pbProgress = itemView.findViewById(R.id.pbProgress);
            layoutFailure = itemView.findViewById(R.id.layoutFailure);
            tvFailureReason = itemView.findViewById(R.id.tvFailureReason);
        }

        void bind(TaskDao.TaskWithState task) {
            TaskDefinition def = task.definition;
            TaskState state = task.state;

            tvTaskType.setText(def.getTaskType().name());
            tvStatus.setText(state.getCurrentState().name());
            tvTaskId.setText("ID: " + def.getTaskId());

            tvLastRun.setText(state.getLastRunTime() > 0 ? sdf.format(new Date(state.getLastRunTime())) : "Never");
            
            long nextRun = state.getNextRunTime();
            if (state.getSnoozedUntil() > System.currentTimeMillis()) {
                tvNextRun.setText(sdf.format(new Date(state.getSnoozedUntil())) + " (Snoozed)");
            } else if (nextRun > 0) {
                tvNextRun.setText(sdf.format(new Date(nextRun)));
            } else {
                tvNextRun.setText("Pending");
            }

            String intervalText = getIntervalTimeFromMilliseconds(def);

            tvInterval.setText(intervalText);

            StringBuilder constraints = new StringBuilder();
            if (def.isRequiresInternet()) constraints.append("WiFi/Cellular, ");
            if (def.getMinimumBattery() > 0) constraints.append("Battery > ").append(def.getMinimumBattery()).append("%");
            tvConstraints.setText(constraints.toString());

            pbProgress.setProgress(state.getProgress());
            tvProgress.setText(state.getProgress() + "%");

            if (state.getLastFailureReason() != null && !state.getLastFailureReason().isEmpty()) {
                layoutFailure.setVisibility(View.VISIBLE);
                tvFailureReason.setText(state.getLastFailureReason());
            } else {
                layoutFailure.setVisibility(View.GONE);
            }
        }
    }

    @NonNull
    private static String getIntervalTimeFromMilliseconds(TaskDefinition def) {
        long millis = def.getRepeatInterval();

        String intervalText;

        if (millis < 60 * 60 * 1000) {
            // Show minutes
            long minutes = millis / (60 * 1000);
            intervalText = minutes + " Minutes";
        } else {
            // Show hours (supports decimals like 1.5)
            double hours = millis / (1000.0 * 60 * 60);

            // Remove .0 for whole numbers
            if (hours == (long) hours) {
                intervalText = ((long) hours) + " Hours";
            } else {
                intervalText = hours + " Hours";
            }
        }
        return intervalText;
    }
}
