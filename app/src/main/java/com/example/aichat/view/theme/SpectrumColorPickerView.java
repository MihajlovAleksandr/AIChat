package com.example.aichat.view.theme;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;

public class SpectrumColorPickerView extends View {

    private final Paint paint =
            new Paint(
                    Paint.ANTI_ALIAS_FLAG
            );

    private final Paint selectorPaint =
            new Paint(
                    Paint.ANTI_ALIAS_FLAG
            );

    private Bitmap bitmap;

    private float hue =
            180f;

    private float saturation =
            1f;

    private float value =
            1f;

    private OnSpectrumColorChangedListener listener;

    public SpectrumColorPickerView(
            Context context
    ) {

        super(
                context
        );

        selectorPaint.setStyle(
                Paint.Style.STROKE
        );

        selectorPaint.setStrokeWidth(
                4f
        );

        selectorPaint.setColor(
                Color.WHITE
        );
    }

    public void setOnColorChangedListener(
            OnSpectrumColorChangedListener listener
    ) {

        this.listener =
                listener;
    }

    public void setColor(
            String color
    ) {

        try {

            int parsed =
                    Color.parseColor(
                            color
                    );

            float[] hsv =
                    new float[3];

            Color.colorToHSV(
                    parsed,
                    hsv
            );

            hue =
                    hsv[0];

            saturation =
                    hsv[1];

            value =
                    hsv[2];

        } catch (Exception ignored) {

            hue =
                    180f;

            saturation =
                    1f;

            value =
                    1f;
        }

        bitmap =
                null;

        invalidate();
    }

    public int getHueInt() {

        return Math.round(
                hue
        );
    }

    public void setHue(
            int hue
    ) {

        this.hue =
                Math.max(
                        0,
                        Math.min(
                                360,
                                hue
                        )
                );

        bitmap =
                null;

        invalidate();

        notifyColorChanged();
    }

    public String getSelectedColorHex() {

        int color =
                Color.HSVToColor(
                        new float[]{
                                hue,
                                saturation,
                                value
                        }
                );

        return String.format(
                Locale.US,
                "#%06X",
                0xFFFFFF & color
        );
    }

    @Override
    protected void onSizeChanged(
            int width,
            int height,
            int oldWidth,
            int oldHeight
    ) {

        super.onSizeChanged(
                width,
                height,
                oldWidth,
                oldHeight
        );

        bitmap =
                null;
    }

    @Override
    protected void onDraw(
            Canvas canvas
    ) {

        super.onDraw(
                canvas
        );

        if (getWidth() <= 0
                || getHeight() <= 0) {

            return;
        }

        if (bitmap == null) {

            bitmap =
                    createSpectrumBitmap(
                            getWidth(),
                            getHeight()
                    );
        }

        canvas.drawBitmap(
                bitmap,
                0,
                0,
                paint
        );

        float selectorX =
                saturation
                        * getWidth();

        float selectorY =
                (1f - value)
                        * getHeight();

        selectorX =
                Math.max(
                        0,
                        Math.min(
                                getWidth(),
                                selectorX
                        )
                );

        selectorY =
                Math.max(
                        0,
                        Math.min(
                                getHeight(),
                                selectorY
                        )
                );

        canvas.drawCircle(
                selectorX,
                selectorY,
                16f,
                selectorPaint
        );

        Paint darkStroke =
                new Paint(
                        Paint.ANTI_ALIAS_FLAG
                );

        darkStroke.setStyle(
                Paint.Style.STROKE
        );

        darkStroke.setStrokeWidth(
                2f
        );

        darkStroke.setColor(
                Color.parseColor(
                        "#66000000"
                )
        );

        canvas.drawCircle(
                selectorX,
                selectorY,
                20f,
                darkStroke
        );
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event
    ) {

        if (event.getAction() != MotionEvent.ACTION_DOWN
                && event.getAction() != MotionEvent.ACTION_MOVE) {

            return true;
        }

        float x =
                Math.max(
                        0,
                        Math.min(
                                getWidth(),
                                event.getX()
                        )
                );

        float y =
                Math.max(
                        0,
                        Math.min(
                                getHeight(),
                                event.getY()
                        )
                );

        saturation =
                getWidth() == 0
                        ? 0f
                        : x / getWidth();

        value =
                getHeight() == 0
                        ? 0f
                        : 1f - y / getHeight();

        notifyColorChanged();

        invalidate();

        return true;
    }

    private Bitmap createSpectrumBitmap(
            int width,
            int height
    ) {

        Bitmap result =
                Bitmap.createBitmap(
                        width,
                        height,
                        Bitmap.Config.ARGB_8888
                );

        int[] pixels =
                new int[
                        width * height
                        ];

        for (int y = 0;
             y < height;
             y++) {

            float currentValue =
                    1f - (
                            y / (float) Math.max(
                                    1,
                                    height - 1
                            )
                    );

            for (int x = 0;
                 x < width;
                 x++) {

                float currentSaturation =
                        x / (float) Math.max(
                                1,
                                width - 1
                        );

                pixels[y * width + x] =
                        Color.HSVToColor(
                                new float[]{
                                        hue,
                                        currentSaturation,
                                        currentValue
                                }
                        );
            }
        }

        result.setPixels(
                pixels,
                0,
                width,
                0,
                0,
                width,
                height
        );

        return result;
    }

    private void notifyColorChanged() {

        if (listener == null) {
            return;
        }

        listener.onColorChanged(
                getSelectedColorHex()
        );
    }

    public interface OnSpectrumColorChangedListener {

        void onColorChanged(
                String color
        );
    }
}
