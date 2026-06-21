package com.example.aichat.view.main.chat.helpers;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

public class VoiceTrimView extends View {

    public interface Listener {
        void onTrimChanged(long startMs, long endMs);

        void onSeekChanged(long positionMs);
    }

    private static final long MIN_SELECTED_MS = 500L;

    private final Paint inactivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playheadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private long durationMs = 0L;
    private long trimStartMs = 0L;
    private long trimEndMs = 0L;
    private long playbackPositionMs = 0L;

    private int activeColor = Color.rgb(32, 163, 154);
    private int inactiveColor = 0x55FFFFFF;
    private int handleColor = Color.WHITE;
    private int textColor = Color.WHITE;
    private int playheadColor = Color.rgb(244, 67, 54);

    private int activeHandle = 0; // 1=start, 2=end, 3=playhead
    private boolean playing = false;
    private float pulse = 0f;
    private float lastTouchX = 0f;
    private ValueAnimator pulseAnimator;
    private Listener listener;

    public VoiceTrimView(Context context) {
        super(context);
        init();
    }

    public VoiceTrimView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VoiceTrimView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inactivePaint.setStyle(Paint.Style.FILL);
        activePaint.setStyle(Paint.Style.FILL);
        handlePaint.setStyle(Paint.Style.FILL);
        playheadPaint.setStyle(Paint.Style.FILL);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(dp(1.5f));
        tickPaint.setStyle(Paint.Style.STROKE);
        tickPaint.setStrokeWidth(dp(1f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dp(10));
        setColors(activeColor, inactiveColor, handleColor, textColor, playheadColor);
    }

    public void setColors(
            int activeColor,
            int inactiveColor,
            int handleColor,
            int textColor,
            int playheadColor
    ) {
        this.activeColor = activeColor;
        this.inactiveColor = inactiveColor;
        this.handleColor = handleColor;
        this.textColor = textColor;
        this.playheadColor = playheadColor;

        activePaint.setColor(activeColor);
        inactivePaint.setColor(inactiveColor);
        handlePaint.setColor(handleColor);
        textPaint.setColor(textColor);
        playheadPaint.setColor(playheadColor);
        pulsePaint.setColor(playheadColor);
        tickPaint.setColor(inactiveColor);
        invalidate();
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setDuration(long durationMs) {
        this.durationMs = Math.max(0L, durationMs);
        this.trimStartMs = 0L;
        this.trimEndMs = this.durationMs;
        this.playbackPositionMs = 0L;
        invalidate();
    }

    public long getTrimStartMs() {
        return trimStartMs;
    }

    public long getTrimEndMs() {
        return trimEndMs > 0L ? trimEndMs : durationMs;
    }

    public void setPlaybackPositionMs(long playbackPositionMs) {
        this.playbackPositionMs = Math.max(trimStartMs, Math.min(playbackPositionMs, getTrimEndMs()));
        invalidate();
    }

    public void setPlaying(boolean playing) {
        if (this.playing == playing) {
            return;
        }

        this.playing = playing;

        if (playing) {
            startPulse();
        } else {
            stopPulse();
        }

        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopPulse();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        float padding = dp(15);
        float barHeight = dp(8);
        float centerY = height * 0.52f;
        float left = padding;
        float right = width - padding;
        float top = centerY - barHeight / 2f;
        float bottom = centerY + barHeight / 2f;

        rect.set(left, top, right, bottom);
        canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, inactivePaint);

        drawTicks(canvas, left, right, centerY, top, bottom);

        float startX = timeToX(trimStartMs, left, right);
        float endX = timeToX(getTrimEndMs(), left, right);

        rect.set(startX, top, endX, bottom);
        canvas.drawRoundRect(rect, barHeight / 2f, barHeight / 2f, activePaint);

        float playX = timeToX(
                Math.max(trimStartMs, Math.min(playbackPositionMs, getTrimEndMs())),
                left,
                right
        );

        canvas.drawRoundRect(
                playX - dp(1.4f),
                top - dp(10),
                playX + dp(1.4f),
                bottom + dp(10),
                dp(1.4f),
                dp(1.4f),
                playheadPaint
        );

        if (playing) {
            pulsePaint.setAlpha((int) (170 * (1f - pulse)));
            canvas.drawCircle(playX, centerY, dp(7) + dp(8) * pulse, pulsePaint);
            pulsePaint.setAlpha(255);
        }

        float handleRadius = getHandleRadius();
        canvas.drawCircle(startX, centerY, handleRadius, handlePaint);
        canvas.drawCircle(endX, centerY, handleRadius, handlePaint);

    }

    private void drawTicks(Canvas canvas, float left, float right, float centerY, float top, float bottom) {
        if (durationMs <= 0L) {
            return;
        }

        int tickCount = durationMs <= 10_000L ? 5 : durationMs <= 30_000L ? 7 : 9;
        tickPaint.setAlpha(95);

        for (int i = 1; i < tickCount; i++) {
            float x = left + (right - left) * i / (float) tickCount;
            float tickHalf = (i % 2 == 0) ? dp(6) : dp(4);
            canvas.drawLine(x, centerY - tickHalf, x, centerY + tickHalf, tickPaint);
        }

        tickPaint.setAlpha(255);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (durationMs <= 0L) {
            return true;
        }

        float padding = dp(15);
        float left = padding;
        float right = getWidth() - padding;
        float x = Math.max(left, Math.min(right, event.getX()));

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activeHandle = chooseHandle(event.getX(), left, right);
                lastTouchX = x;
                if (activeHandle == 3) {
                    updatePlayheadAbsolute(x, left, right, true);
                }
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (activeHandle == 1 || activeHandle == 2) {
                    updateTrimHandleByDelta(x - lastTouchX, left, right, true);
                    lastTouchX = x;
                } else {
                    updatePlayheadAbsolute(x, left, right, true);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activeHandle == 1 || activeHandle == 2) {
                    updateTrimHandleByDelta(x - lastTouchX, left, right, true);
                } else if (activeHandle == 3) {
                    updatePlayheadAbsolute(x, left, right, true);
                }
                activeHandle = 0;
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            default:
                return true;
        }
    }

    private int chooseHandle(float x, float left, float right) {
        float startX = timeToX(trimStartMs, left, right);
        float endX = timeToX(getTrimEndMs(), left, right);
        float playX = timeToX(
                Math.max(trimStartMs, Math.min(playbackPositionMs, getTrimEndMs())),
                left,
                right
        );

        float handleTouchRadius = getTouchRadius();
        float playheadTouchRadius = dp(18);

        float distStart = Math.abs(x - startX);
        float distEnd = Math.abs(x - endX);
        float distPlay = Math.abs(x - playX);

        if (distStart <= handleTouchRadius || distEnd <= handleTouchRadius) {
            return distStart <= distEnd ? 1 : 2;
        }

        if (distPlay <= playheadTouchRadius) {
            return 3;
        }

        return 3;
    }

    private void updateTrimHandleByDelta(float dx, float left, float right, boolean notify) {
        if (Math.abs(dx) < 0.1f) {
            return;
        }

        long deltaMs = deltaXToTime(dx, left, right);

        if (activeHandle == 1) {
            trimStartMs = Math.max(0L, Math.min(trimStartMs + deltaMs, getTrimEndMs() - MIN_SELECTED_MS));
            playbackPositionMs = Math.max(trimStartMs, Math.min(playbackPositionMs, getTrimEndMs()));

            if (listener != null && notify) {
                listener.onTrimChanged(trimStartMs, getTrimEndMs());
            }
        } else if (activeHandle == 2) {
            trimEndMs = Math.min(durationMs, Math.max(getTrimEndMs() + deltaMs, trimStartMs + MIN_SELECTED_MS));
            playbackPositionMs = Math.max(trimStartMs, Math.min(playbackPositionMs, getTrimEndMs()));

            if (listener != null && notify) {
                listener.onTrimChanged(trimStartMs, getTrimEndMs());
            }
        }

        invalidate();
    }

    private void updatePlayheadAbsolute(float x, float left, float right, boolean notify) {
        long value = xToTime(x, left, right);
        playbackPositionMs = Math.max(trimStartMs, Math.min(value, getTrimEndMs()));

        if (listener != null && notify) {
            listener.onSeekChanged(playbackPositionMs);
        }

        invalidate();
    }

    private long deltaXToTime(float dx, float left, float right) {
        float progressDelta = dx / Math.max(1f, right - left);
        float sensitivity = trimSensitivity();
        return (long) (durationMs * progressDelta * sensitivity);
    }

    private float trimSensitivity() {
        if (durationMs <= 10_000L) return 1f;
        if (durationMs <= 20_000L) return 0.72f;
        if (durationMs <= 45_000L) return 0.48f;
        return 0.34f;
    }

    private float getHandleRadius() {
        if (durationMs >= 30_000L) return dp(11.5f);
        return dp(10.5f);
    }

    private float getTouchRadius() {
        if (durationMs >= 30_000L) return dp(30f);
        if (durationMs >= 15_000L) return dp(26f);
        return dp(22f);
    }

    private float timeToX(long timeMs, float left, float right) {
        if (durationMs <= 0L) {
            return left;
        }

        float progress = Math.max(0f, Math.min(1f, timeMs / (float) durationMs));
        return left + (right - left) * progress;
    }

    private long xToTime(float x, float left, float right) {
        float progress = (x - left) / Math.max(1f, right - left);
        progress = Math.max(0f, Math.min(1f, progress));
        return (long) (durationMs * progress);
    }

    private void startPulse() {
        stopPulse();
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(760L);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(animation -> {
            pulse = (float) animation.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    private void stopPulse() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        pulse = 0f;
    }

    private String format(long ms) {
        long seconds = Math.max(0L, ms / 1000L);
        long minutes = seconds / 60L;
        long rest = seconds % 60L;
        return minutes + ":" + (rest < 10 ? "0" : "") + rest;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
