package com.example.pp.features.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.pp.R;
import java.util.ArrayList;
import java.util.List;

public class HeatmapView extends View {
    private Paint cellPaint;
    private Paint textPaint;
    private int[] heatColors;
    private List<Integer> data = new ArrayList<>();
    private float spacing = 8f;
    private float labelWidth = 60f;
    private float legendHeight = 40f;
    private RectF rect = new RectF();

    public HeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_muted));
        textPaint.setTextSize(24f);
        
        heatColors = new int[]{
                ContextCompat.getColor(getContext(), R.color.surface_container),
                0xFF0E4429,
                0xFF006D32,
                0xFF26A641,
                ContextCompat.getColor(getContext(), R.color.emerald_green)
        };
        // Dummy data for 4 weeks (28 days)
        for (int i = 0; i < 28; i++) {
            data.add((int) (Math.random() * 5));
        }
    }

    public void setData(List<Integer> newData) {
        this.data = newData;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data.isEmpty()) return;

        float width = getWidth();
        float height = getHeight() - legendHeight;
        
        float availableWidth = width - labelWidth - spacing;
        float cellSize = Math.min((availableWidth - (7 * spacing)) / 7f, (height - (5 * spacing)) / 4f);

        // Draw Week labels and cells
        for (int i = 0; i < data.size(); i++) {
            int row = i / 7;
            int col = i % 7;

            // Draw "Wn" label only once per row
            if (col == 0) {
                float labelY = row * (cellSize + spacing) + spacing + (cellSize / 2f) + (textPaint.getTextSize() / 3f);
                canvas.drawText("W" + (row + 1), spacing, labelY, textPaint);
            }

            float left = labelWidth + col * (cellSize + spacing) + spacing;
            float top = row * (cellSize + spacing) + spacing;
            rect.set(left, top, left + cellSize, top + cellSize);

            int level = data.get(i);
            cellPaint.setColor(heatColors[Math.min(level, heatColors.length - 1)]);
            
            if (level >= 4) {
                cellPaint.setShadowLayer(8, 0, 0, ContextCompat.getColor(getContext(), R.color.emerald_green));
            } else {
                cellPaint.clearShadowLayer();
            }

            canvas.drawRoundRect(rect, 6f, 6f, cellPaint);
        }

        // Draw Legend at bottom right
        float legendCellSize = 20f;
        float legendSpacing = 4f;
        float legendWidth = (heatColors.length * (legendCellSize + legendSpacing)) + 120f;
        float legendX = width - legendWidth;
        float legendY = height + (legendHeight / 2f);

        textPaint.setTextSize(20f);
        canvas.drawText("Less", legendX, legendY + 5, textPaint);
        
        float startCells = legendX + 50f;
        for (int i = 0; i < heatColors.length; i++) {
            cellPaint.setColor(heatColors[i]);
            cellPaint.clearShadowLayer();
            float left = startCells + i * (legendCellSize + legendSpacing);
            float top = legendY - (legendCellSize / 2f);
            rect.set(left, top, left + legendCellSize, top + legendCellSize);
            canvas.drawRoundRect(rect, 2f, 2f, cellPaint);
        }
        
        canvas.drawText("More", startCells + (heatColors.length * (legendCellSize + legendSpacing)) + 5f, legendY + 5, textPaint);
    }
}
