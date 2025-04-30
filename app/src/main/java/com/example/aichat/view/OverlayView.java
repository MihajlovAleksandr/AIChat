package com.example.aichat.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class OverlayView extends View {
    private Paint paint;
    private Rect scanAreaRect;

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        scanAreaRect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Размеры области сканирования (70% ширины экрана)
        int width = (int)(getWidth() * 0.7);
        int height = width; // Квадратная область

        // Центрируем область сканирования
        scanAreaRect.left = (getWidth() - width) / 2;
        scanAreaRect.top = (getHeight() - height) / 2;
        scanAreaRect.right = scanAreaRect.left + width;
        scanAreaRect.bottom = scanAreaRect.top + height;

        // Рисуем прямоугольник области сканирования
        canvas.drawRect(scanAreaRect, paint);

        // Добавляем уголки (опционально)
        drawCorners(canvas, scanAreaRect);
    }

    private void drawCorners(Canvas canvas, Rect rect) {
        float cornerLength = 50f;
        Paint cornerPaint = new Paint();
        cornerPaint.setColor(Color.GREEN);
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(8f);

        // Левый верхний угол
        canvas.drawLine(
                rect.left, rect.top,
                rect.left, rect.top + cornerLength,
                cornerPaint);
        canvas.drawLine(
                rect.left, rect.top,
                rect.left + cornerLength, rect.top,
                cornerPaint);

        // Правый верхний угол
        canvas.drawLine(
                rect.right, rect.top,
                rect.right, rect.top + cornerLength,
                cornerPaint);
        canvas.drawLine(
                rect.right, rect.top,
                rect.right - cornerLength, rect.top,
                cornerPaint);

        // Левый нижний угол
        canvas.drawLine(
                rect.left, rect.bottom,
                rect.left, rect.bottom - cornerLength,
                cornerPaint);
        canvas.drawLine(
                rect.left, rect.bottom,
                rect.left + cornerLength, rect.bottom,
                cornerPaint);

        // Правый нижний угол
        canvas.drawLine(
                rect.right, rect.bottom,
                rect.right, rect.bottom - cornerLength,
                cornerPaint);
        canvas.drawLine(
                rect.right, rect.bottom,
                rect.right - cornerLength, rect.bottom,
                cornerPaint);
    }

    public Rect getScanAreaRect() {
        return scanAreaRect;
    }
}