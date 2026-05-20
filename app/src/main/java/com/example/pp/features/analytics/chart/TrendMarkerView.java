package com.example.pp.features.analytics.chart;

import android.content.Context;
import android.widget.TextView;

import com.example.pp.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TrendMarkerView extends MarkerView {

    private final TextView tvDate;
    private final TextView tvAmount;
    private final SimpleDateFormat dateFormat;
    private final Calendar calendar;
    private List<Long> dateTimestamps;
    private List<String> xLabels;

    public TrendMarkerView(Context context) {
        super(context, R.layout.marker_trend);
        tvDate = findViewById(R.id.tvMarkerDate);
        tvAmount = findViewById(R.id.tvMarkerAmount);
        dateFormat = new SimpleDateFormat("d MMM", Locale.getDefault());
        calendar = Calendar.getInstance();
    }

    public void setDateTimestamps(List<Long> timestamps) {
        this.dateTimestamps = timestamps;
        this.xLabels = null;
    }

    public void setXLabels(List<String> labels) {
        this.xLabels = labels;
        this.dateTimestamps = null;
    }

    public void setDatePattern(String pattern) {
        dateFormat.applyPattern(pattern);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        if (dateTimestamps != null && index >= 0 && index < dateTimestamps.size()) {
            calendar.setTimeInMillis(dateTimestamps.get(index));
            tvDate.setText(dateFormat.format(calendar.getTime()));
        } else if (xLabels != null && index >= 0 && index < xLabels.size()) {
            tvDate.setText(xLabels.get(index));
        } else {
            tvDate.setText("");
        }
        tvAmount.setText(String.format(Locale.getDefault(), "\u20B9 %,.0f", e.getY()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 16f);
    }
}
