package com.example.aichat.view.main.chat.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

public class VoiceWaveformView extends View {

    private static final int MAX_BARS = 42;

    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Deque<Float> amplitudes = new ArrayDeque<>();

    private int activeColor = 0xFFFFFFFF;
    private int inactiveColor = 0x55FFFFFF;
    private boolean paused = false;

    public VoiceWaveformView(Context context) {
        super(context);
        init();
    }

    public VoiceWaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VoiceWaveformView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        activePaint.setStrokeCap(Paint.Cap.ROUND);
        inactivePaint.setStrokeCap(Paint.Cap.ROUND);
        activePaint.setColor(activeColor);
        inactivePaint.setColor(inactiveColor);
    }

    public void setColors(int activeColor, int inactiveColor) {
        this.activeColor = activeColor;
        this.inactiveColor = inactiveColor;
        activePaint.setColor(activeColor);
        inactivePaint.setColor(inactiveColor);
        invalidate();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        invalidate();
    }

    public void clear() {
        amplitudes.clear();
        invalidate();
    }

    public void addAmplitude(int rawAmplitude) {
        float normalized = Math.min(1f, Math.max(0.04f, rawAmplitude / 32767f));

        if (paused) {
            normalized = 0.04f;
        }

        amplitudes.addLast(normalized);

        while (amplitudes.size() > MAX_BARS) {
            amplitudes.removeFirst();
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float barWidth = 3f * density;
        float gap = 3f * density;
        float step = barWidth + gap;
        float centerY = height / 2f;
        float maxBarHeight = height * 0.82f;

        activePaint.setStrokeWidth(barWidth);
        inactivePaint.setStrokeWidth(barWidth);

        int expectedBars = Math.max(1, Math.min(MAX_BARS, (int) (width / step)));
        int missing = expectedBars - amplitudes.size();
        float x = width - barWidth / 2f;

        Float[] values = amplitudes.toArray(new Float[0]);

        for (int i = values.length - 1; i >= 0; i--) {
            float value = values[i] != null ? values[i] : 0.04f;
            float barHeight = Math.max(barWidth, maxBarHeight * value);
            Paint paint = paused ? inactivePaint : activePaint;
            canvas.drawLine(x, centerY - barHeight / 2f, x, centerY + barHeight / 2f, paint);
            x -= step;

            if (x < 0) {
                break;
            }
        }

        for (int i = 0; i < missing && x >= 0; i++) {
            float barHeight = Math.max(barWidth, maxBarHeight * 0.04f);
            canvas.drawLine(x, centerY - barHeight / 2f, x, centerY + barHeight / 2f, inactivePaint);
            x -= step;
        }
    }
}
