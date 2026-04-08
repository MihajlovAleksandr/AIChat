package com.example.aichat.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class OverlayView extends View {

    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dimPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scanLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Rect scanAreaRect = new Rect();
    private final RectF rectF = new RectF();

    private float scanLineY = 0f;
    private boolean scanLineGoingDown = true;
    private boolean highlightDetectedQR = false;
    private float frameScale = 1f;

    private final Handler handler = new Handler();

    public OverlayView(@NonNull Context context) { super(context); init(); }
    public OverlayView(@NonNull Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public OverlayView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(6f);

        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(10f);

        dimPaint.setColor(Color.argb(120, 0, 0, 0));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(48f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        scanLinePaint.setAntiAlias(true);
    }

    public void highlightDetectedQRStatic() {
        highlightDetectedQR = true;
        frameScale = 1.15f;
        invalidate();

        handler.postDelayed(() -> {
            highlightDetectedQR = false;
            frameScale = 1f;
            invalidate();
        }, 500);
    }

    public Rect getScanAreaRect() {
        return new Rect(scanAreaRect);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int baseWidth = (int) (w * 0.7f);
        scanAreaRect.left = (w - baseWidth) / 2;
        scanAreaRect.top = (h - baseWidth) / 2;
        scanAreaRect.right = scanAreaRect.left + baseWidth;
        scanAreaRect.bottom = scanAreaRect.top + baseWidth;

        rectF.set(scanAreaRect);

        LinearGradient scanGradient = new LinearGradient(
                scanAreaRect.left, scanAreaRect.top,
                scanAreaRect.right, scanAreaRect.bottom,
                new int[]{Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        scanLinePaint.setShader(scanGradient);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int baseWidth = (int) (getWidth() * 0.7);
        int scaledWidth = (int) (baseWidth * frameScale);

        scanAreaRect.left = (getWidth() - scaledWidth) / 2;
        scanAreaRect.top = (getHeight() - scaledWidth) / 2;
        scanAreaRect.right = scanAreaRect.left + scaledWidth;
        scanAreaRect.bottom = scanAreaRect.top + scaledWidth;

        rectF.set(scanAreaRect);

        canvas.drawRect(0, 0, getWidth(), rectF.top, dimPaint);
        canvas.drawRect(0, rectF.bottom, getWidth(), getHeight(), dimPaint);
        canvas.drawRect(0, rectF.top, rectF.left, rectF.bottom, dimPaint);
        canvas.drawRect(rectF.right, rectF.top, getWidth(), rectF.bottom, dimPaint);

        if (highlightDetectedQR) {
            framePaint.setColor(Color.GREEN);
            cornerPaint.setColor(Color.GREEN);
        } else {
            framePaint.setColor(Color.WHITE);
            cornerPaint.setColor(Color.WHITE);
        }

        canvas.drawRoundRect(rectF, 40f, 40f, framePaint);
        drawCorners(canvas, scanAreaRect);
        if (scanLineGoingDown) {
            scanLineY += 8f;
            if (scanLineY >= scaledWidth) scanLineGoingDown = false;
        } else {
            scanLineY -= 8f;
            if (scanLineY <= 0) scanLineGoingDown = true;
        }
        float y = scanAreaRect.top + scanLineY;
        canvas.drawLine(scanAreaRect.left + 20, y, scanAreaRect.right - 20, y, scanLinePaint);

        canvas.drawText("Разместите код внутри рамки", getWidth() / 2f, scanAreaRect.bottom + 80, textPaint);

        postInvalidateOnAnimation();
    }

    private void drawCorners(Canvas canvas, Rect rect) {
        float L = 50f;

        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + L, cornerPaint);
        canvas.drawLine(rect.left, rect.top, rect.left + L, rect.top, cornerPaint);

        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + L, cornerPaint);
        canvas.drawLine(rect.right, rect.top, rect.right - L, rect.top, cornerPaint);

        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - L, cornerPaint);
        canvas.drawLine(rect.left, rect.bottom, rect.left + L, rect.bottom, cornerPaint);

        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - L, cornerPaint);
        canvas.drawLine(rect.right, rect.bottom, rect.right - L, rect.bottom, cornerPaint);
    }
}