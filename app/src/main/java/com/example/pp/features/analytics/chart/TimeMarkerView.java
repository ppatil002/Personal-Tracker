package com.example.pp.features.analytics.chart;

import android.content.Context;
import android.widget.TextView;

import com.example.pp.R;
import com.example.pp.utils.DateUtils;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.Locale;

public class TimeMarkerView extends MarkerView {

    public enum DisplayMode { TIME, COUNT }

    private final TextView tvDate;
    private final TextView tvAmount;
    private String[] labels;
    private DisplayMode displayMode = DisplayMode.TIME;

    public TimeMarkerView(Context context) {
        super(context, R.layout.marker_trend);
        tvDate = findViewById(R.id.tvMarkerDate);
        tvAmount = findViewById(R.id.tvMarkerAmount);
    }

    public void setLabels(String[] labels) {
        this.labels = labels;
    }

    public void setDisplayMode(DisplayMode mode) {
        this.displayMode = mode;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        if (labels != null && index >= 0 && index < labels.length) {
            tvDate.setText(labels[index]);
        } else {
            tvDate.setText("");
        }
        
        if (displayMode == DisplayMode.TIME) {
            tvAmount.setText(DateUtils.convertTimestampIntoHoursAndMinutes((long) e.getY()));
        } else {
            tvAmount.setText(String.valueOf((int) e.getY()));
        }
        super.refreshContent(e, highlight);
    }


    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 16f);
    }
}
