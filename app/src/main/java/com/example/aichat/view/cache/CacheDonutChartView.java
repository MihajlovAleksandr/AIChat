package com.example.aichat.view.cache;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.aichat.model.utils.files.FileCacheStats;
import java.util.ArrayList;
import java.util.List;

public class CacheDonutChartView extends View {

    public static final class Slice {
        public final long bytes;
        public final int color;
        public final String label;

        public Slice(long bytes, int color, @NonNull String label) {
            this.bytes = Math.max(0L, bytes);
            this.color = color;
            this.label = label;
        }
    }

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final List<Slice> slices = new ArrayList<>();

    private long totalBytes;
    private int centerTextColor = Color.WHITE;
    private int centerSubTextColor = 0x99FFFFFF;
    private int centerFillColor = 0xFF101820;

    public CacheDonutChartView(Context context) {
        super(context);
        init();
    }

    public CacheDonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CacheDonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);

        centerPaint.setStyle(Paint.Style.FILL);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        subtitlePaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setThemeColors(int centerFillColor, int centerTextColor, int centerSubTextColor) {
        this.centerFillColor = centerFillColor;
        this.centerTextColor = centerTextColor;
        this.centerSubTextColor = centerSubTextColor;
        invalidate();
    }

    public void setSlices(@Nullable List<Slice> newSlices) {
        slices.clear();
        totalBytes = 0L;

        if (newSlices != null) {
            for (Slice slice : newSlices) {
                if (slice == null || slice.bytes <= 0) {
                    continue;
                }

                slices.add(slice);
                totalBytes += slice.bytes;
            }
        }

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desired = dp(220);
        int width = resolveSize(desired, widthMeasureSpec);
        int height = resolveSize(desired, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float size = Math.min(width, height);
        float stroke = size * 0.18f;
        float padding = stroke * 0.65f;
        float centerX = width / 2f;
        float centerY = height / 2f;
        float radius = (size / 2f) - padding;

        arcPaint.setStrokeWidth(stroke);
        arcBounds.set(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
        );

        if (totalBytes <= 0 || slices.isEmpty()) {
            arcPaint.setColor(0x335A7080);
            canvas.drawArc(arcBounds, -90f, 360f, false, arcPaint);
        } else {
            float start = -90f;
            for (Slice slice : slices) {
                float sweep = (slice.bytes * 360f) / totalBytes;
                if (sweep < 2f) {
                    sweep = 2f;
                }
                arcPaint.setColor(slice.color);
                canvas.drawArc(arcBounds, start, sweep, false, arcPaint);
                start += sweep;
            }
        }

        centerPaint.setColor(centerFillColor);
        canvas.drawCircle(centerX, centerY, radius - stroke * 0.72f, centerPaint);

        String main = totalBytes > 0 ? FileCacheStats.formatBytes(totalBytes).replace(" ", "\n") : "0\nB";
        String[] lines = main.split("\\n");

        textPaint.setColor(centerTextColor);
        textPaint.setTextSize(size * 0.17f);
        canvas.drawText(lines[0], centerX, centerY - dp(2), textPaint);

        subtitlePaint.setColor(centerSubTextColor);
        subtitlePaint.setTextSize(size * 0.075f);
        String unit = lines.length > 1 ? lines[1] : "";
        canvas.drawText(unit, centerX, centerY + dp(24), subtitlePaint);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
