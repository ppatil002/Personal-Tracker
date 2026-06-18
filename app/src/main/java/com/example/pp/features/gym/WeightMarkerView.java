package com.example.pp.features.gym;

import android.content.Context;
import android.widget.TextView;

import com.example.pp.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeightMarkerView extends MarkerView {

    private final TextView tvDate;
    private final TextView tvWeight;
    private final SimpleDateFormat dateFormat;
    private List<Long> dateTimestamps;

    public WeightMarkerView(Context context, List<Long> dateTimestamps) {
        super(context, R.layout.marker_trend);
        this.dateTimestamps = dateTimestamps;
        tvDate = findViewById(R.id.tvMarkerDate);
        tvWeight = findViewById(R.id.tvMarkerAmount);
        dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        if (dateTimestamps != null && index >= 0 && index < dateTimestamps.size()) {
            tvDate.setText(dateFormat.format(new Date(dateTimestamps.get(index))));
        } else {
            tvDate.setText("");
        }
        tvWeight.setText(String.format(Locale.getDefault(), "%.1f kg", e.getY()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 16f);
    }
}
