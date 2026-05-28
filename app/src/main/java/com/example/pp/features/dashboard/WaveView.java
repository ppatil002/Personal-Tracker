package com.example.pp.features.dashboard;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.pp.R;

public class WaveView extends View {
    private Paint wavePaint;
    private Paint circlePaint;
    private Path wavePath;
    private float progress = 0.6f; // Default 60%
    private float waveOffset = 0f;
    private ValueAnimator animator;

    public WaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setColor(ContextCompat.getColor(getContext(), R.color.hydration_blue));
        wavePaint.setAlpha(102); // 40% opacity as per Figma

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(4f);
        circlePaint.setColor(ContextCompat.getColor(getContext(), R.color.hydration_blue));
        circlePaint.setAlpha(76); // 30% opacity

        wavePath = new Path();

        animator = ValueAnimator.ofFloat(0, 1f);
        animator.setDuration(5000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            waveOffset = (float) animation.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        animator.start();
    }

    public void setProgress(float progress) {
        this.progress = Math.min(1f, Math.max(0f, progress));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float radius = Math.min(width, height) / 2f;
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Clip to circle
        wavePath.reset();
        wavePath.addCircle(centerX, centerY, radius, Path.Direction.CW);
        canvas.clipPath(wavePath);

        // Draw background circle stroke
        canvas.drawCircle(centerX, centerY, radius - 2, circlePaint);

        // Draw wave
        wavePath.reset();
        float waveHeight = 20f;
        float waveWidth = width;
        float fillLevel = height * (1f - progress);

        wavePath.moveTo(-waveWidth + (waveOffset * waveWidth), fillLevel);
        for (float x = -waveWidth; x <= waveWidth * 2; x += 5) {
            float y = (float) (fillLevel + waveHeight * Math.sin((x + waveOffset * waveWidth) * 2 * Math.PI / waveWidth));
            wavePath.lineTo(x, y);
        }
        wavePath.lineTo(width * 2, height);
        wavePath.lineTo(-waveWidth, height);
        wavePath.close();

        canvas.drawPath(wavePath, wavePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }
}
