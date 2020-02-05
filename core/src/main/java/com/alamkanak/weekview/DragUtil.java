package com.alamkanak.weekview;

import android.graphics.PointF;
import android.os.CountDownTimer;
import android.view.MotionEvent;

import java.time.ZonedDateTime;
import java.util.Calendar;

public class DragUtil {

    private boolean isDragging = false, longPressTimerCancelled = false, longPressTimerRunning = false;
    private CountDownTimer longPressTimer = null;
    private EventDragListener<T> eventDragListener;
    private PointF dragStartPoint = new PointF();

    Calendar drag_init_start_time = Calendar.getInstance();
    Calendar drag_init_end_time = Calendar.getInstance();

    Calendar drag_start_time = Calendar.getInstance();
    Calendar drag_end_time = Calendar.getInstance();

    boolean dragStartTimeSet = false;
    private int roundOffTimeMinutes = 15;

    boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        final boolean val = gestureDetector.onTouchEvent(event);


        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isDragging) {
                getEventDragListener().onDragOver(drag_start_time, drag_end_time);

            }
            dragOver();

        }


        // Check after call of gestureDetector, so currentFlingDirection and currentScrollDirection are set
        if (event.getAction() == ACTION_UP && !isZooming && currentFlingDirection == Direction.NONE) {
            if (currentScrollDirection == Direction.RIGHT || currentScrollDirection == Direction.LEFT) {
                goToNearestOrigin();
            }
            currentScrollDirection = Direction.NONE;
        }


        if (getEventDragListener() != null && event.getAction() == MotionEvent.ACTION_MOVE) {
            //Check if user is actually longpressing, not slow-moving
            // if current position differs much then press positon then discard whole thing
            // If position change is minimal then after 0.5s that is a longpress. You can now process your other gestures

            if (isDragging) {
                //drag and show view

                if (!(event.getX() > config.getTimeColumnWidth() && event.getY() > config.getHeaderHeight()))
                    return val;


                ZonedDateTime selectedTime = touchHandler.getTimeFromPoint(event);

                Calendar selectedCal = Calendar.getInstance();
                if (selectedTime != null)
                    selectedCal = toCalendar(selectedTime);

                if (selectedTime != null) {


                    if (!dragStartTimeSet) {
                        drag_init_start_time.setTimeInMillis(roundOffTime(toCalendar(selectedTime), true).getTimeInMillis());
                        drag_init_end_time.setTimeInMillis(roundOffTime(toCalendar(selectedTime), false).getTimeInMillis());
                        dragStartTimeSet = true;
                    }

                    if (isCloseToQuarter(drag_init_start_time, drag_init_end_time, selectedCal)) {
                        callDrag(drag_init_start_time, drag_init_end_time);
                    } else {

                        if (selectedCal.getTimeInMillis() > drag_init_end_time.getTimeInMillis()) {
                            Calendar updated_end_time = roundOffTime(selectedCal, false);
                            callDrag(drag_init_start_time, updated_end_time);
                        } else {
                            Calendar updated_start_time = roundOffTime(selectedCal, true);
                            callDrag(updated_start_time, drag_init_end_time);
                        }


                    }


                }


//                scrollWhileDrag(event);

            } else {

//                handleLongPressManually(event);
            }


        }


        return val;
    }

    public void callDrag(Calendar start_cal, Calendar end_cal) {
        drag_start_time = start_cal;
        drag_end_time = end_cal;
        getEventDragListener().onDragging(drag_start_time, drag_end_time);
    }



    private void dragOver() {

        isDragging = false;
        dragStartTimeSet = false;
        removeLongPressTimer();

    }

    private void removeLongPressTimer() {

        if (longPressTimer != null)
            longPressTimer.cancel();

        longPressTimer = null;
        isDragging = false;
        longPressTimerRunning = false;
        longPressTimerCancelled = false;
    }


    public Calendar roundOffTime(Calendar cal, boolean getNearestToStart) {
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.get(Calendar.MINUTE) >= 0 && cal.get(Calendar.MINUTE) <= 15) {
            if (getNearestToStart)
                cal.set(Calendar.MINUTE, 0);
            else
                cal.set(Calendar.MINUTE, 15);

        } else if (cal.get(Calendar.MINUTE) > 15 && cal.get(Calendar.MINUTE) <= 30) {
            if (getNearestToStart)
                cal.set(Calendar.MINUTE, 15);
            else
                cal.set(Calendar.MINUTE, 30);
        } else if (cal.get(Calendar.MINUTE) > 30 && cal.get(Calendar.MINUTE) <= 45) {
            if (getNearestToStart)
                cal.set(Calendar.MINUTE, 30);
            else
                cal.set(Calendar.MINUTE, 45);
        } else if (cal.get(Calendar.MINUTE) > 45) {
            if (getNearestToStart)
                cal.set(Calendar.MINUTE, 45);
            else
                cal.set(Calendar.MINUTE, 60);
        }

        return cal;
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void setDragging(boolean dragging) {
        isDragging = dragging;
    }

    public EventDragListener<T> getEventDragListener() {
        return eventDragListener;
    }

    public void setEventDragListener(EventDragListener<T> eventDragListener) {
        this.eventDragListener = eventDragListener;
    }
}
