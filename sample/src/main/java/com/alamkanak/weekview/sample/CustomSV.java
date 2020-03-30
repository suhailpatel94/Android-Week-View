package com.alamkanak.weekview.sample;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

public class CustomSV extends NestedScrollView {

    private boolean isTouchModeEnabled = true;

    public CustomSV(@NonNull Context context) {
        super(context);
    }

    public CustomSV(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomSV(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // if we can scroll pass the event to the superclass
                return isTouchModeEnabled && super.onTouchEvent(ev);
            default:
                return super.onTouchEvent(ev);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Don't do anything with intercepted touch events if
        // we are not scrollable
        return isTouchModeEnabled && super.onInterceptTouchEvent(ev);
    }
    public boolean isTouchModeEnabled() {
        return isTouchModeEnabled;
    }

    public void setTouchModeEnabled(boolean touchModeEnabled) {
        isTouchModeEnabled = touchModeEnabled;
    }
}
