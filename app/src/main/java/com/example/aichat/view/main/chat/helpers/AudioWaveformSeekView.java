package com.example.aichat.view.main.chat.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AudioWaveformSeekView extends View {

    public static final String VOICE_WAVEFORM_TAG = "voice_waveform_seek_view";

    public interface OnSeekListener {
        void onSeek(float progress);
    }

    private final Paint inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float[] levels = new float[0];
    private float progress = 0f;
    private boolean userScrubbing = false;

    @Nullable
    private OnSeekListener onSeekListener;

    public AudioWaveformSeekView(@NonNull Context context) {
        super(context);
        init();
    }

    public AudioWaveformSeekView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioWaveformSeekView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(false);

        activePaint.setStrokeCap(Paint.Cap.ROUND);
        inactivePaint.setStrokeCap(Paint.Cap.ROUND);

        activePaint.setColor(0xFF20A39A);
        inactivePaint.setColor(0x66808080);
    }

    public void setColors(int activeColor, int inactiveColor) {
        activePaint.setColor(activeColor);
        inactivePaint.setColor(inactiveColor);
        invalidate();
    }

    public void setLevels(@Nullable float[] source) {
        if (source == null || source.length == 0) {
            levels = new float[0];
        } else {
            levels = source;
        }

        invalidate();
    }

    public void setProgress(float value) {
        float safeValue = Math.max(0f, Math.min(1f, value));

        if (Math.abs(progress - safeValue) < 0.001f) {
            return;
        }

        progress = safeValue;
        invalidate();
    }

    public boolean isUserScrubbing() {
        return userScrubbing;
    }

    public void setOnSeekListener(@Nullable OnSeekListener listener) {
        onSeekListener = listener;
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
        float barWidth = Math.max(2f * density, width / 180f);
        float gap = Math.max(2f * density, barWidth * 0.9f);
        float step = barWidth + gap;
        int count = Math.max(8, Math.min(levels.length > 0 ? levels.length : 48, (int) (width / step)));

        float usedWidth = (count - 1) * step + barWidth;
        float startX = (width - usedWidth) / 2f + barWidth / 2f;
        float centerY = height / 2f;
        float maxBarHeight = height * 0.78f;
        float activeEdge = width * progress;

        activePaint.setStrokeWidth(barWidth);
        inactivePaint.setStrokeWidth(barWidth);

        for (int i = 0; i < count; i++) {
            float level = levelForIndex(i, count);
            float barHeight = Math.max(barWidth, maxBarHeight * level);
            float x = startX + i * step;
            Paint paint = x <= activeEdge ? activePaint : inactivePaint;
            canvas.drawLine(x, centerY - barHeight / 2f, x, centerY + barHeight / 2f, paint);
        }
    }

    private float levelForIndex(int index, int visibleCount) {
        if (levels.length == 0) {
            return 0.18f + 0.42f * (float) Math.abs(Math.sin(index * 0.51f));
        }

        if (visibleCount <= 1) {
            return levels[0];
        }

        int mappedIndex = Math.round(index * (levels.length - 1) / (float) (visibleCount - 1));
        mappedIndex = Math.max(0, Math.min(levels.length - 1, mappedIndex));
        return Math.max(0.08f, Math.min(1f, levels[mappedIndex]));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                userScrubbing = true;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                updateProgressFromTouch(event.getX(), false);
                return true;

            case MotionEvent.ACTION_MOVE:
                updateProgressFromTouch(event.getX(), false);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                updateProgressFromTouch(event.getX(), event.getActionMasked() == MotionEvent.ACTION_UP);
                userScrubbing = false;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private void updateProgressFromTouch(float x, boolean notify) {
        float width = Math.max(1f, getWidth());
        progress = Math.max(0f, Math.min(1f, x / width));
        invalidate();

        if (notify && onSeekListener != null) {
            onSeekListener.onSeek(progress);
        }
    }
}
