package com.example.aichat.view.main.chatlist;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LeaderboardIconDrawable extends Drawable {

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = Color.WHITE;
    private int alpha = 255;

    public LeaderboardIconDrawable() {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        float size = Math.min(bounds.width(), bounds.height());

        if (size <= 0f) {
            return;
        }

        float left = bounds.left + (bounds.width() - size) / 2f;
        float top = bounds.top + (bounds.height() - size) / 2f;
        float stroke = Math.max(1.8f, size * 0.075f);
        float radius = size * 0.08f;

        strokePaint.setStrokeWidth(stroke);
        strokePaint.setColor(color);
        strokePaint.setAlpha(alpha);
        fillPaint.setColor(color);
        fillPaint.setAlpha(alpha);

        float baseY = top + size * 0.82f;
        float barWidth = size * 0.16f;
        float gap = size * 0.08f;
        float centerX = left + size * 0.5f;
        float middleLeft = centerX - barWidth / 2f;
        float leftBarLeft = middleLeft - barWidth - gap;
        float rightBarLeft = middleLeft + barWidth + gap;

        drawBar(canvas, leftBarLeft, top + size * 0.48f, leftBarLeft + barWidth, baseY, radius);
        drawBar(canvas, middleLeft, top + size * 0.30f, middleLeft + barWidth, baseY, radius);
        drawBar(canvas, rightBarLeft, top + size * 0.58f, rightBarLeft + barWidth, baseY, radius);

        canvas.drawLine(
                left + size * 0.21f,
                baseY + stroke * 1.2f,
                left + size * 0.79f,
                baseY + stroke * 1.2f,
                strokePaint
        );

        float dotRadius = Math.max(1.5f, size * 0.035f);
        canvas.drawCircle(centerX, top + size * 0.18f, dotRadius, fillPaint);
        canvas.drawCircle(centerX - size * 0.17f, top + size * 0.27f, dotRadius, fillPaint);
        canvas.drawCircle(centerX + size * 0.17f, top + size * 0.37f, dotRadius, fillPaint);
    }

    private void drawBar(Canvas canvas, float left, float top, float right, float bottom, float radius) {
        RectF rect = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(rect, radius, radius, strokePaint);
    }

    @Override
    public void setTint(int tintColor) {
        color = tintColor;
        invalidateSelf();
    }

    @Override
    public void setTintList(@Nullable ColorStateList tint) {
        if (tint != null) {
            color = tint.getColorForState(getState(), tint.getDefaultColor());
            invalidateSelf();
        }
    }

    @Override
    protected boolean onStateChange(int[] state) {
        invalidateSelf();
        return true;
    }

    @Override
    public boolean isStateful() {
        return true;
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        strokePaint.setColorFilter(colorFilter);
        fillPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 34;
    }

    @Override
    public int getIntrinsicHeight() {
        return 34;
    }
}
