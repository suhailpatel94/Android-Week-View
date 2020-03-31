package com.alamkanak.weekview.sample;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.alamkanak.weekview.OnEmptyViewLongClickListener;
import com.alamkanak.weekview.PointCalendarWrapper;
import com.alamkanak.weekview.WeekView;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.temporal.ChronoUnit;

import java.util.Calendar;

public class DragUtil {

    ConstraintLayout dragView;
    View scaleTop, scaleBottom;
    FrameLayout drag_container;

    Float last_touch_x, last_touch_y;
    WeekView mWeekView;
    Context context;

    CustomSV parent;

    DragCompleted dragCompleted;


    public DragUtil(CustomSV parent, FrameLayout drag_container, WeekView mWeekView, Context context) {
        this.drag_container = drag_container;
        this.mWeekView = mWeekView;
        this.context = context;
        this.parent = parent;
    }

    public void init() {

        drag_container.setOnDragListener(new DragUtil.MyDragListener());


        mWeekView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                last_touch_x = event.getX();
                last_touch_y = event.getY();
                return false;
            }
        });

        mWeekView.setOnEmptyViewLongClickListener(new OnEmptyViewLongClickListener() {
            @Override
            public void onEmptyViewLongClick(@NotNull Calendar calendar) {
                Log.e("QQAA", "LONGCLICK");
                PointCalendarWrapper drag_view_top = mWeekView.getSnappedPixel(last_touch_x, last_touch_y, true);


//                PointCalendarWrapper drag_view_bottom = mWeekView.getSnappedPixel(last_touch_x, last_touch_y, false);
                if (drag_view_top != null) {
                    Log.e("DRAGDD", "W = " + drag_view_top.getWidth() + " || X = " + drag_view_top.getX() + " || Y=" + drag_view_top.getY());

                    long minutesintoday = getMinutesFromBeginning(drag_view_top.getCalendar());
                    Float bottom_y;
                    if (minutesintoday < 1410) {
                        bottom_y = mWeekView.getYPixelFromMinutes(minutesintoday + 30);
                        create(drag_view_top.getX(), drag_view_top.getY(), drag_view_top.getWidth(), (int) (bottom_y - drag_view_top.getY()));
                    } else {
                        Float top_y = mWeekView.getYPixelFromMinutes(1410);
                        bottom_y = mWeekView.getYPixelFromMinutes(1440);
                        create(drag_view_top.getX(), top_y, drag_view_top.getWidth(), (int) (bottom_y - top_y));
                    }


                }


            }
        });


    }

    public void removeDragView() {
        if (dragView != null) {
            drag_container.removeView(dragView);

        }
    }


    public long getMinutesFromBeginning(Calendar calendar) {
        ZonedDateTime zdt = DateTimeUtils.toZonedDateTime(calendar);
        long minutesintoday = ChronoUnit.MINUTES.between(zdt.toLocalDate().atStartOfDay(), zdt);

        Log.e("DRAGDD_MINUTES", String.valueOf(minutesintoday));
        Log.e("DRAGDD_Y_FROM_MIN", String.valueOf(mWeekView.getYPixelFromMinutes(minutesintoday)));
        return minutesintoday;
    }

    public void create(float x, float y, int width, int height) {
        removeDragView();
        dragView = (ConstraintLayout) LayoutInflater.from(context).inflate(R.layout.item_drag, null);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        dragView.setLayoutParams(params);
        dragView.setX(x);
        dragView.setY(y);

        drag_container.addView(dragView);
        vibrate();
        onDragUpdate();
//            dragView.setOnTouchListener(MyTouchListener);

        dragView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

//            View.DragShadowBuilder mShadow = new View.DragShadowBuilder();
                View.DragShadowBuilder mShadow = new MyDragShadow(v);

                ClipData.Item item = new ClipData.Item("");
                String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};
                ClipData data = new ClipData("", mimeTypes, item);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    v.startDragAndDrop(data, mShadow, v, 0);
                } else {
                    v.startDrag(data, mShadow, v, 0);
                }

                return false;
            }
        });

        scaleTop = dragView.findViewById(R.id.scaleTop);
        scaleBottom = dragView.findViewById(R.id.scaleBottom);

        scaleTop.setOnTouchListener(new ScaleTouchListener());
        scaleBottom.setOnTouchListener(new ScaleTouchListener());
    }

    public ConstraintLayout getDragView() {
        return dragView;
    }

    public View getScaleTop() {
        return scaleTop;
    }

    public View getScaleBottom() {
        return scaleBottom;
    }

    class MyDragShadow extends View.DragShadowBuilder {
        int shadowWidth;
        int shadowHeight;
        Paint paint;

        public MyDragShadow(View v) {
            super(v);
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        @Override
        public void onProvideShadowMetrics(Point shadowSize,
                                           Point shadowTouchPoint) {
            shadowWidth = getView().getWidth();
            shadowHeight = getView().getHeight();
            shadowSize.set(shadowWidth, shadowHeight);

            shadowTouchPoint.set((int) shadowWidth / 2, (int) shadowWidth / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {

        }
    }

    private final class ScaleTouchListener implements View.OnTouchListener {

        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
//                dragViewUtil.getDragView().setOnTouchListener(null);
                parent.setTouchModeEnabled(false);
                return true;
            }

            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                PointCalendarWrapper pointCalendarWrapper;
                if (view.getId() == R.id.scaleTop) {

                    pointCalendarWrapper = mWeekView.getSnappedPixel(getDragView().getX(), getDragView().getY(), true);
                } else {
                    pointCalendarWrapper = mWeekView.getSnappedPixel(getDragView().getX(), getDragView().getY() + getDragView().getHeight(), false);
                }


                if (view.getId() == R.id.scaleTop) {

                    float previous_y = getDragView().getY();
                    getDragView().setY(pointCalendarWrapper.getY());
                    float updated_y = getDragView().getY();
                    float height_diff = previous_y - updated_y;

                    getDragView().getLayoutParams().height = (int) (getDragView().getLayoutParams().height + height_diff);
                    getDragView().requestLayout();
                } else {
                    float height_diff = pointCalendarWrapper.getY() - getDragView().getY() - getDragView().getHeight();
                    getDragView().getLayoutParams().height = (int) (getDragView().getLayoutParams().height + height_diff);
                    getDragView().requestLayout();
                }
                parent.setTouchModeEnabled(true);
                onDragUpdate();
//                dragViewUtil.getDragView().setOnTouchListener(MyTouchListener);
                return true;
            }


            if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {

                resizeView(view.getId() == R.id.scaleTop, motionEvent.getX(), motionEvent.getY());
                return true;
            } else {
                return true;
            }

        }
    }

    class MyDragListener implements View.OnDragListener {


        @Override
        public boolean onDrag(View v, DragEvent event) {
            View view = (View) event.getLocalState();

            switch (event.getAction()) {

                case DragEvent.ACTION_DRAG_STARTED:
                    vibrate();
                    return true;

                case DragEvent.ACTION_DRAG_LOCATION:

                    if (view.getId() == R.id.draggable) {
                        int new_topY = (int) event.getY() - (getDragView().getHeight() / 2);
                        int new_bottomY = new_topY + getDragView().getHeight();

                        if (new_topY < mWeekView.getTopLimit()) {
                            return true;
                        }

                        if (new_bottomY > drag_container.getHeight()) {
                            return false;
                        }

                        view.setY(new_topY);
                        view.setX(view.getX());
                        view.requestLayout();

                    }

                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:

                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    v.invalidate();
                    return true;

                case DragEvent.ACTION_DROP:

                    if (view.getId() == R.id.draggable) {
                        // Dropped, reassign View to ViewGroup
                        view = (View) event.getLocalState();
                        PointCalendarWrapper pointF = mWeekView.getSnappedPixel(view.getX(), view.getY(), true);

                        view.setY(pointF.getY());
                        onDragUpdate();
                    }


                    return true;


                case DragEvent.ACTION_DRAG_ENDED:
                    return true;

                default:
                    return false;

            }
        }
    }

    public void resizeView(boolean is_top_drag, float x, float y) {
        int new_height;

        int previous_height = getDragView().getHeight();
        if (is_top_drag) {
            new_height = (int) (getDragView().getLayoutParams().height - y);
        } else {
            new_height = (int) (getDragView().getLayoutParams().height + y);
        }

        //validate
        boolean isValid = false;
        float new_top_y;
        float new_bottom_y;
        if (is_top_drag) {
            int offset = previous_height - new_height;
            new_top_y = getDragView().getY() + offset;
            new_bottom_y = new_top_y + new_height;
        } else {
            new_top_y = getDragView().getY();
            new_bottom_y = new_top_y + new_height;
        }

        if (new_top_y >= mWeekView.getTopLimit() && new_bottom_y <= drag_container.getHeight())
            isValid = true;

        if (isValid) {
            getDragView().getLayoutParams().height = new_height;
            if (is_top_drag) {
                int offset = previous_height - new_height;
                getDragView().setY(getDragView().getY() + offset);
            }

            getDragView().requestLayout();

        }


    }

    public void onDragUpdate() {


//        isVibrationDone = false;
        float top_y = getDragView().getY();
        float bottom_y = getDragView().getY() + getDragView().getHeight();

        Calendar start = mWeekView.getSnappedPixel(dragView.getX(), top_y, true).getCalendar();
        Calendar end = mWeekView.getSnappedPixel(dragView.getX(), bottom_y, false).getCalendar();

        if (dragCompleted != null) {
            dragCompleted.onDragCompleted(start, end);
        }
    }

    public void vibrate() {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(500);
        }
    }

    public interface DragCompleted {
        void onDragCompleted(Calendar start, Calendar end);
    }

    public void setOnDragCompleted(DragCompleted dragCompleted) {
        this.dragCompleted = dragCompleted;
    }

}
