package com.example.pp.ui.components.timeline;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pp.R;

import java.util.ArrayList;
import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_INTAKE = 0;
    private static final int TYPE_GAP = 1;

    public interface TimelineItem {}

    public static class IntakeItem implements TimelineItem {
        public String time;
        public String amount;
        public String subtitle;
        public String stats;
        public boolean isRecent;

        public IntakeItem(String time, String amount, String subtitle, String stats, boolean isRecent) {
            this.time = time;
            this.amount = amount;
            this.subtitle = subtitle;
            this.stats = stats;
            this.isRecent = isRecent;
        }
    }

    public static class GapItem implements TimelineItem {
        public String gapLabel;
        public boolean isWarning;

        public GapItem(String gapLabel, boolean isWarning) {
            this.gapLabel = gapLabel;
            this.isWarning = isWarning;
        }
    }

    private List<TimelineItem> items = new ArrayList<>();

    public void setItems(List<TimelineItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof IntakeItem) return TYPE_INTAKE;
        return TYPE_GAP;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_INTAKE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline_intake, parent, false);
            return new IntakeViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline_gap, parent, false);
            return new GapViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof IntakeViewHolder) {
            ((IntakeViewHolder) holder).bind((IntakeItem) items.get(position));
        } else {
            ((GapViewHolder) holder).bind((GapItem) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class IntakeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTitle, tvSubtitle, tvStats;
        View timelineLine;

        IntakeViewHolder(View view) {
            super(view);
            tvTime = view.findViewById(R.id.tvIntakeTime);
            tvTitle = view.findViewById(R.id.tvIntakeTitle);
            tvSubtitle = view.findViewById(R.id.tvIntakeSubtitle);
            tvStats = view.findViewById(R.id.tvIntakeStats);
            timelineLine = view.findViewById(R.id.timelineLine);
        }

        void bind(IntakeItem item) {
            tvTime.setText(item.time);
            tvTitle.setText(item.amount);
            if (item.subtitle != null) {
                tvSubtitle.setText(item.subtitle);
                tvSubtitle.setVisibility(View.VISIBLE);
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }
            if (item.stats != null) {
                tvStats.setText(item.stats);
                tvStats.setVisibility(View.VISIBLE);
            } else {
                tvStats.setVisibility(View.GONE);
            }
        }
    }

    static class GapViewHolder extends RecyclerView.ViewHolder {
        TextView tvLabel;
        View timelineLine;

        GapViewHolder(View view) {
            super(view);
            tvLabel = view.findViewById(R.id.tvGapLabel);
            timelineLine = view.findViewById(R.id.timelineLine);
        }

        void bind(GapItem item) {
            if (item.isWarning) {
                tvLabel.setText("⚠️ " + item.gapLabel);
                tvLabel.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.error_red));
            } else {
                tvLabel.setText(item.gapLabel);
                tvLabel.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }
        }
    }
}
