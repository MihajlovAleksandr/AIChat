package com.example.aichat.view.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

public class TouchContainer extends FrameLayout {

    private float startX;
    private float startY;
    private final int touchSlop;
    private boolean isVertical;

    public TouchContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                isVertical = false;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - startX);
                float dy = Math.abs(ev.getY() - startY);

                if (dy > touchSlop && dy > dx) {
                    isVertical = true;
                    return true;
                }

                return false;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return isVertical;
    }
}
