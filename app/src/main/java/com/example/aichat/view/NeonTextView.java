package com.example.aichat.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;

import androidx.appcompat.widget.AppCompatTextView;

import com.example.aichat.R;

public class NeonTextView extends AppCompatTextView {

    private Paint basePaint;
    private Paint shimmerPaint;
    private LinearGradient shimmerGradient;
    private final Matrix matrix = new Matrix();
    private float shimmerX = 0f;

    private int baseColor;
    private int brightColor;
    private float glowRadius = 30f;
    private float glowIntensity = 1.0f;
    private long shimmerDuration = 3000L;
    private boolean shimmerEnabled = true;

    private ValueAnimator shimmerAnimator;

    public NeonTextView(Context context) {
        this(context, null);
    }

    public NeonTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NeonTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        initColors(attrs);
        initPaints();
    }


    @SuppressLint("Recycle")
    private void initColors(AttributeSet attrs) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.NeonTextView,
                0, 0
        );
        try {
            baseColor = a.getColor(R.styleable.NeonTextView_neonBaseColor, 0xFF42969E);
            brightColor = a.getColor(R.styleable.NeonTextView_neonBrightColor, 0xFF6ED5DE);
        } finally {
            a.recycle();
        }
    }

    private void initPaints() {
        basePaint = new Paint(getPaint());
        basePaint.setColor(baseColor);

        shimmerPaint = new Paint(getPaint());
        shimmerPaint.setColor(brightColor);
        updateGlowLayer();
    }


    @SuppressWarnings("unused")
    public void setNeonColors(int baseColor, int brightColor) {
        this.baseColor = baseColor;
        this.brightColor = brightColor;
        basePaint.setColor(baseColor);
        shimmerPaint.setColor(brightColor);
        updateGradient(getWidth());
        updateGlowLayer();
        invalidate();
    }

    @SuppressWarnings("unused")
    public void setShimmerSpeed(long durationMs) {
        this.shimmerDuration = durationMs;
        restartShimmer();
    }

    @SuppressWarnings("unused")
    public void setGlowRadius(float radius) {
        this.glowRadius = radius;
        updateGlowLayer();
        invalidate();
    }

    @SuppressWarnings("unused")
    public void setGlowIntensity(float intensity) {
        this.glowIntensity = Math.max(0f, Math.min(1f, intensity));
        updateGlowLayer();
        invalidate();
    }

    @SuppressWarnings("unused")
    public void setShimmerEnabled(boolean enabled) {
        this.shimmerEnabled = enabled;
        if (!enabled && shimmerAnimator != null) shimmerAnimator.cancel();
        else if (enabled) restartShimmer();
    }


    private void updateGlowLayer() {
        int glowColor = adjustAlpha(brightColor, glowIntensity);
        shimmerPaint.setShadowLayer(glowRadius, 0f, 0f, glowColor);
    }

    private void updateGradient(int width) {
        if (width <= 0) return;

        shimmerGradient = new LinearGradient(
                -width, 0,
                0, 0,
                new int[]{
                        adjustAlpha(baseColor, 0f),
                        adjustAlpha(baseColor, 0.53f),
                        brightColor,
                        adjustAlpha(baseColor, 0.53f),
                        adjustAlpha(baseColor, 0f)
                },
                new float[]{0f, 0.35f, 0.5f, 0.65f, 1f},
                Shader.TileMode.CLAMP
        );
        shimmerPaint.setShader(shimmerGradient);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(android.graphics.Color.alpha(color) * factor);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void restartShimmer() {
        if (!shimmerEnabled) return;
        if (shimmerAnimator != null) shimmerAnimator.cancel();

        shimmerAnimator = ValueAnimator.ofFloat(-getWidth(), getWidth() * 2f);
        shimmerAnimator.setDuration(shimmerDuration);
        shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        shimmerAnimator.setInterpolator(new LinearInterpolator());
        shimmerAnimator.addUpdateListener(animation -> {
            shimmerX = (float) animation.getAnimatedValue();
            invalidate();
        });
        shimmerAnimator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradient(w);
        if (shimmerEnabled) restartShimmer();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        String text = getText() != null ? getText().toString() : "";
        float x = getPaddingLeft();
        float y = getBaseline();

        canvas.drawText(text, x, y, basePaint);

        if (shimmerEnabled && shimmerGradient != null) {
            matrix.setTranslate(shimmerX, 0);
            shimmerGradient.setLocalMatrix(matrix);
            canvas.drawText(text, x, y, shimmerPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
            shimmerAnimator = null;
        }
    }
}