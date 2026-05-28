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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HeatmapView extends View {

    private Paint cellPaint;
    private Paint textPaint;

    private int[] heatColors;

    private List<Integer> data = new ArrayList<>();

    private final RectF rect = new RectF();

    // Reduced spacing for dense GitHub-like feel
    private final float spacing = 6f;

    // Outer card padding
    private final float outerPadding = 24f;

    // Width reserved for week labels
    private final float weekLabelWidth = 64f;

    // Space reserved for legend
    private final float legendHeight = 70f;

    public HeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        // Needed for glow shadow rendering
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(
                ContextCompat.getColor(getContext(), R.color.text_muted)
        );
        textPaint.setTextSize(18f);

        // AMOLED Neon Futuristic Palette
        heatColors = new int[]{
                ContextCompat.getColor(getContext(), R.color.heatmap_empty),
                ContextCompat.getColor(getContext(), R.color.heatmap_level_1),
                ContextCompat.getColor(getContext(), R.color.heatmap_level_2),
                ContextCompat.getColor(getContext(), R.color.heatmap_level_3),
                ContextCompat.getColor(getContext(), R.color.heatmap_level_4)
        };

        // Dummy data
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

        if (data == null || data.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();

        int columns = 7;
        int rows = (int) Math.ceil(data.size() / 7f);

        // -----------------------------
        // AVAILABLE SPACE
        // -----------------------------

        float availableWidth =
                width
                        - (2 * outerPadding)
                        - weekLabelWidth;

        float availableHeight =
                height
                        - legendHeight
                        - outerPadding;

        // -----------------------------
        // CELL SIZE CALCULATION
        // -----------------------------

        float cellSizeFromWidth =
                (availableWidth - ((columns - 1) * spacing))
                        / columns;

        float cellSizeFromHeight =
                (availableHeight - ((rows - 1) * spacing))
                        / rows;

        float cellSize =
                Math.min(cellSizeFromWidth, cellSizeFromHeight);

        // -----------------------------
        // GRID SIZE
        // -----------------------------

        float gridWidth =
                weekLabelWidth
                        + (columns * cellSize)
                        + ((columns - 1) * spacing);

        float gridHeight =
                (rows * cellSize)
                        + ((rows - 1) * spacing);

        // -----------------------------
        // CENTER ENTIRE GRAPH
        // -----------------------------

        float startX =
                (width - gridWidth) / 2f;

        float startY =
                outerPadding;

        // -----------------------------
        // CURRENT WEEK NUMBER
        // -----------------------------

        Calendar calendar =
                Calendar.getInstance(Locale.getDefault());

        int currentWeek =
                calendar.get(Calendar.WEEK_OF_YEAR);

        // -----------------------------
        // DRAW HEATMAP
        // -----------------------------

        for (int i = 0; i < data.size(); i++) {

            int row = i / columns;
            int col = i % columns;

            // -----------------------------
            // DRAW WEEK LABEL
            // -----------------------------

            if (col == 0) {

                int weekNumber =
                        currentWeek - row;

                float labelY =
                        startY
                                + row * (cellSize + spacing)
                                + (cellSize / 2f)
                                + 6f;

                canvas.drawText(
                        "W" + weekNumber,
                        startX,
                        labelY,
                        textPaint
                );
            }

            // -----------------------------
            // CELL POSITION
            // -----------------------------

            float left =
                    startX
                            + weekLabelWidth
                            + col * (cellSize + spacing);

            float top =
                    startY
                            + row * (cellSize + spacing);

            rect.set(
                    left,
                    top,
                    left + cellSize,
                    top + cellSize
            );

            int level =
                    data.get(i);

            int color =
                    heatColors[
                            Math.min(level, heatColors.length - 1)
                            ];

            cellPaint.setColor(color);

            // -----------------------------
            // GLOW EFFECT
            // -----------------------------

            if (level >= 4) {

                cellPaint.setShadowLayer(
                        12f,
                        0f,
                        0f,
                        color
                );

            } else {

                cellPaint.clearShadowLayer();
            }

            // -----------------------------
            // DRAW CELL
            // -----------------------------

            canvas.drawRoundRect(
                    rect,
                    6f,
                    6f,
                    cellPaint
            );
        }

        // -----------------------------
        // LEGEND
        // -----------------------------

        float legendCellSize = 22f;

        float legendSpacing = 6f;

        float gap = 12f;

        float legendY =
                startY + gridHeight + 42f;

        float lessWidth =
                textPaint.measureText("Less");

        float moreWidth =
                textPaint.measureText("More");

        float cellsWidth =
                heatColors.length * legendCellSize
                        + ((heatColors.length - 1) * legendSpacing);

        float totalLegendWidth =
                lessWidth
                        + gap
                        + cellsWidth
                        + gap
                        + moreWidth;

        // Right align legend relative to graph
        float legendStartX =
                startX
                        + gridWidth
                        - totalLegendWidth;

        // -----------------------------
        // DRAW LESS TEXT
        // -----------------------------

        canvas.drawText(
                "Less",
                legendStartX,
                legendY + 6f,
                textPaint
        );

        float cellsStartX =
                legendStartX + lessWidth + gap;

        // -----------------------------
        // DRAW LEGEND CELLS
        // -----------------------------

        for (int i = 0; i < heatColors.length; i++) {

            cellPaint.setColor(heatColors[i]);

            cellPaint.clearShadowLayer();

            float left =
                    cellsStartX
                            + i * (legendCellSize + legendSpacing);

            float top =
                    legendY - (legendCellSize / 2f);

            rect.set(
                    left,
                    top,
                    left + legendCellSize,
                    top + legendCellSize
            );

            canvas.drawRoundRect(
                    rect,
                    4f,
                    4f,
                    cellPaint
            );
        }

        // -----------------------------
        // DRAW MORE TEXT
        // -----------------------------

        canvas.drawText(
                "More",
                cellsStartX + cellsWidth + gap,
                legendY + 6f,
                textPaint
        );
    }
}