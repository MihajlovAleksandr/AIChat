package com.example.aichat.util;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.example.aichat.view.main.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class HoneycombRevealView extends View {

    private static final int HEX_SIZE = 140;
    private static final long ANIM_DURATION = 650;

    private final Paint paintFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGlow = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<HexCell> cells = new ArrayList<>();

    private float progress = 0f;
    private boolean initialized = false;
    private boolean reverse = false;

    public HoneycombRevealView(Context context) {
        super(context);
        init();
    }

    public HoneycombRevealView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HoneycombRevealView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
        paintFill.setColor(Color.WHITE);
        paintFill.setStyle(Paint.Style.FILL);

        paintGlow.setColor(Color.parseColor("#42969E"));
        paintGlow.setStyle(Paint.Style.STROKE);
        paintGlow.setStrokeWidth(6f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        generateHoneycomb(w, h);
        initialized = true;
    }

    private void generateHoneycomb(int width, int height) {
        cells.clear();

        float hexH = HEX_SIZE * 0.866f;
        int cols = (int) (width / (HEX_SIZE * 0.75f)) + 3;
        int rows = (int) (height / hexH) + 3;

        float cx0 = width / 2f;
        float cy0 = height / 2f;

        for (int row = -1; row < rows; row++) {
            for (int col = -1; col < cols; col++) {

                float cx = col * HEX_SIZE * 0.75f;
                float cy = row * hexH + ((col % 2 == 0) ? 0 : hexH / 2);

                Path hex = createHexagon(cx, cy, HEX_SIZE / 2f);

                float dx = cx - cx0;
                float dy = cy - cy0;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                float appearTime = dist / 1800f;

                cells.add(new HexCell(hex, appearTime, cx, cy));
            }
        }
    }

    private Path createHexagon(float cx, float cy, float r) {
        Path p = new Path();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            float x = cx + (float) (r * Math.cos(angle));
            float y = cy + (float) (r * Math.sin(angle));
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!initialized) return;

        for (HexCell cell : cells) {
            boolean visible = !reverse
                    ? progress >= cell.appearTime
                    : progress <= (1f - cell.appearTime);

            if (visible) {
                float scale = reverse ? progress : 1f - progress;
                float alpha = Math.min(1f, Math.max(0f, scale));

                paintFill.setAlpha((int) (255 * alpha));
                paintGlow.setAlpha((int) (255 * alpha));

                canvas.save();
                canvas.scale(scale, scale, cell.cx, cell.cy);
                canvas.drawPath(cell.path, paintFill);
                canvas.drawPath(cell.path, paintGlow);
                canvas.restore();
            }
        }
    }

    public void start(boolean reverseMode, Runnable onEnd) {
        this.reverse = reverseMode;

        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(ANIM_DURATION);
        anim.setInterpolator(new DecelerateInterpolator());

        anim.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });

        anim.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animator) {}
            @Override public void onAnimationCancel(Animator animator) {}
            @Override public void onAnimationRepeat(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
                if (getContext() instanceof Activity) {
                    Activity activity = (Activity) getContext();
                    Intent intent = new Intent(activity, MainActivity.class);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(0, 0);
                    activity.finish();
                }

                if (onEnd != null) onEnd.run();
            }
        });

        anim.start();
    }

    private static class HexCell {
        Path path;
        float appearTime;
        float cx, cy;

        HexCell(Path p, float t, float cx, float cy) {
            path = p;
            appearTime = t;
            this.cx = cx;
            this.cy = cy;
        }
    }
}