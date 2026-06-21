package com.example.aichat.view.main.chat.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class CircleClipFrameLayout extends FrameLayout {

    private final Path clipPath = new Path();
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int lastWidth = -1;
    private int lastHeight = -1;
    private float strokeWidthPx = 0f;

    public CircleClipFrameLayout(@NonNull Context context) {
        super(context);
        init();
    }

    public CircleClipFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircleClipFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        setClipChildren(true);
        setClipToPadding(true);

        strokeWidthPx = getResources().getDisplayMetrics().density;
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidthPx);
        strokePaint.setColor(resolveThemeColor(com.google.android.material.R.attr.colorPrimary, 0xFF20A39A));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setClipToOutline(true);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    int size = Math.min(view.getWidth(), view.getHeight());
                    int left = (view.getWidth() - size) / 2;
                    int top = (view.getHeight() - size) / 2;
                    outline.setOval(left, top, left + size, top + size);
                }
            });
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        updatePathIfNeeded();

        int save = canvas.save();
        canvas.clipPath(clipPath);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    @Override
    public void draw(Canvas canvas) {
        updatePathIfNeeded();

        int save = canvas.save();
        canvas.clipPath(clipPath);
        super.draw(canvas);
        canvas.restoreToCount(save);

        drawCircleStroke(canvas);
    }

    private void updatePathIfNeeded() {
        int width = getWidth();
        int height = getHeight();

        if (width == lastWidth && height == lastHeight) {
            return;
        }

        lastWidth = width;
        lastHeight = height;
        clipPath.reset();

        float radius = Math.min(width, height) / 2f;
        clipPath.addCircle(width / 2f, height / 2f, radius, Path.Direction.CW);
        clipPath.close();
    }

    private void drawCircleStroke(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0 || strokeWidthPx <= 0f) {
            return;
        }

        float radius = Math.min(width, height) / 2f - strokeWidthPx / 2f;
        canvas.drawCircle(width / 2f, height / 2f, Math.max(0f, radius), strokePaint);
    }

    private int resolveThemeColor(int attr, int fallback) {
        TypedValue value = new TypedValue();

        boolean resolved = getContext() != null
                && getContext().getTheme() != null
                && getContext().getTheme().resolveAttribute(attr, value, true);

        return resolved ? value.data : fallback;
    }
}
