package com.alamkanak.weekview


import android.util.Log
import android.view.*
import android.view.MotionEvent.ACTION_UP
import android.widget.OverScroller
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.alamkanak.weekview.Direction.*
import java.util.*
import kotlin.math.*


private enum class Direction {
    NONE, LEFT, RIGHT, VERTICAL;

    val isHorizontal: Boolean
        get() = this == LEFT || this == RIGHT

    val isLeft: Boolean
        get() = this == LEFT

    val isRight: Boolean
        get() = this == RIGHT

    val isVertical: Boolean
        get() = this == VERTICAL

    val isNotNone: Boolean
        get() = this != NONE
}

internal class WeekViewGestureHandler<T : Any>(
        private val view: WeekView<*>,
        private val config: WeekViewConfigWrapper,
        private val chipCache: EventChipCache<T>,
        private val listener: Listener
) : GestureDetector.SimpleOnGestureListener() {

    private val touchHandler = WeekViewTouchHandler(config)

    private val scroller = OverScroller(view.context, FastOutLinearInInterpolator())
    private var currentScrollDirection = NONE
    private var currentFlingDirection = NONE

    private val gestureDetector = GestureDetector(view.context, this)

    private val scaleDetector = ScaleGestureDetector(view.context,
            object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    isZooming = false
                    listener.requireInvalidation()
                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    isZooming = true
                    goToNearestOrigin()
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val hourHeight = config.hourHeight
                    config.newHourHeight = hourHeight * detector.scaleFactor
                    listener.requireInvalidation()
                    return true
                }
            })

    private var isZooming: Boolean = false

    private val minimumFlingVelocity = view.scaledMinimumFlingVelocity
    private val scaledTouchSlop = view.scaledTouchSlop

    var onEventClickListener: OnEventClickListener<T>? = null
    var onEventLongClickListener: OnEventLongClickListener<T>? = null

    var onEmptyViewClickListener: OnEmptyViewClickListener? = null
    var onEmptyViewLongClickListener: OnEmptyViewLongClickListener? = null

    var eventDragBeginListener: EventDragBeginListener? = null
        get() = field
        set(value) {
            Log.e("CALLED", "CALLED")
            field = value
            setDragStatus()
        }
    var eventDraggingListener: EventDraggingListener? = null
        get() = field
        set(value) {
            field = value
            setDragStatus()
        }
    var eventDragOverListener: EventDragOverListener? = null
        get() = field
        set(value) {
            field = value
            setDragStatus()
        }

    var scrollListener: ScrollListener? = null

    private var isDraggingGoingOn = false
    private lateinit var drag_init_start_time: Calendar
    private lateinit var drag_init_end_time: Calendar

    private lateinit var drag_start_time: Calendar
    private lateinit var drag_end_time: Calendar

    private var dragStartTimeSet = false
    private var isDragEnabled = false
    var snapMinutes = 1
        get() = field
        set(value) {
            if (60 % value != 0) {
                throw IllegalArgumentException("snap value should be a factor of 60")
            } else
                field = value
        }


    override fun onDown(
            e: MotionEvent
    ): Boolean {
        goToNearestOrigin()
        return true
    }

    override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
    ): Boolean {
        if (isZooming) {
            return true
        }

        val absDistanceX = abs(distanceX)
        val absDistanceY = abs(distanceY)

        val canScrollHorizontally = config.horizontalScrollingEnabled

        when (currentScrollDirection) {
            NONE -> {
                // Allow scrolling only in one direction.
                currentScrollDirection = if (absDistanceX > absDistanceY && canScrollHorizontally) {
                    if (distanceX > 0) LEFT else RIGHT
                } else {
                    Direction.VERTICAL
                }
            }
            LEFT -> {
                // Change direction if there was enough change.
                if (absDistanceX > absDistanceY && distanceX < -scaledTouchSlop) {
                    currentScrollDirection = RIGHT
                }
            }
            RIGHT -> {
                // Change direction if there was enough change.
                if (absDistanceX > absDistanceY && distanceX > scaledTouchSlop) {
                    currentScrollDirection = LEFT
                }
            }
            else -> Unit
        }

        // Calculate the new origin after scroll.
        when {
            currentScrollDirection.isHorizontal -> {
                config.currentOrigin.x -= distanceX * config.xScrollingSpeed
                config.currentOrigin.x = min(config.currentOrigin.x, config.maxX)
                config.currentOrigin.x = max(config.currentOrigin.x, config.minX)
                listener.requireInvalidation()
            }
            currentScrollDirection.isVertical -> {
                config.currentOrigin.y -= distanceY
                listener.requireInvalidation()
            }
            else -> Unit
        }

        return true
    }

    override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
    ): Boolean {
        if (isZooming) {
            return true
        }

        val isHorizontalAndDisabled =
                currentFlingDirection.isHorizontal && !config.horizontalFlingEnabled

        val isVerticalAndDisabled = currentFlingDirection.isVertical && !config.verticalFlingEnabled

        if (isHorizontalAndDisabled || isVerticalAndDisabled) {
            return true
        }

        scroller.forceFinished(true)

        currentFlingDirection = currentScrollDirection
        when {
            currentFlingDirection.isHorizontal -> onFlingHorizontal(velocityX)
            currentFlingDirection.isVertical -> onFlingVertical(velocityY)
            else -> Unit
        }

        listener.requireInvalidation()
        return true
    }

    private fun onFlingHorizontal(
            originalVelocityX: Float
    ) {
        val startX = config.currentOrigin.x.toInt()
        val startY = config.currentOrigin.y.toInt()

        val velocityX = (originalVelocityX * config.xScrollingSpeed).toInt()
        val velocityY = 0

        val minX = config.minX.toInt()
        val maxX = config.maxX.toInt()

        val dayHeight = config.hourHeight * config.hoursPerDay
        val viewHeight = view.height

        val minY = (dayHeight + config.headerHeight - viewHeight).toInt() * -1
        val maxY = 0

        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
    }

    private fun onFlingVertical(
            originalVelocityY: Float
    ) {
        val startX = config.currentOrigin.x.toInt()
        val startY = config.currentOrigin.y.toInt()

        val velocityX = 0
        val velocityY = originalVelocityY.toInt()

        val minX = Int.MIN_VALUE
        val maxX = Int.MAX_VALUE

        val dayHeight = config.hourHeight * config.hoursPerDay
        val viewHeight = view.height

        val minY = (dayHeight + config.headerHeight - viewHeight).toInt() * -1
        val maxY = 0

        scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
    }

    override fun onSingleTapConfirmed(
            e: MotionEvent
    ): Boolean {
        onEventClickListener?.let { listener ->
            val eventChip = findHitEvent(e.x, e.y) ?: return@let
            if (eventChip.event.isNotAllDay && e.isInHeader) {
                // The user tapped in the header area and a single event that is rendered below it
                // has recognized the tap. We ignore this.
                return@let
            }

            val data = eventChip.originalEvent.data ?: throw NullPointerException(
                    "Did you pass the original object into the constructor of WeekViewEvent?")

            val rect = checkNotNull(eventChip.bounds)
            listener.onEventClick(data, rect)

            return super.onSingleTapConfirmed(e)
        }

        // If the tap was on in an empty space, then trigger the callback.
        val timeColumnWidth = config.timeColumnWidth
        val isWithinCalendarArea = e.x > timeColumnWidth && e.y > config.headerHeight

        if (onEmptyViewClickListener != null && isWithinCalendarArea) {
            val selectedTime = touchHandler.calculateTimeFromPoint(e.x, e.y)
            if (selectedTime != null) {
                onEmptyViewClickListener?.onEmptyViewClicked(selectedTime)
            }
        }

        return super.onSingleTapConfirmed(e)
    }

    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)

        if (isDragEnabled) {
            eventDragBeginListener?.onDragBegin();

            isDraggingGoingOn = true;
        }

        onEventLongClickListener?.let { listener ->
            val eventChip = findHitEvent(e.x, e.y) ?: return@let
            if (eventChip.event.isNotAllDay && e.isInHeader) {
                // The user tapped in the header area and a single event that is rendered below it
                // has recognized the tap. We ignore this.
                return@let
            }

            val data = eventChip.originalEvent.data ?: throw NullPointerException(
                    "Did you pass the original object into the constructor of WeekViewEvent?")

            val rect = checkNotNull(eventChip.bounds)
            listener.onEventLongClick(data, rect)
            return
        }

        val timeColumnWidth = config.timeColumnWidth

        // If the tap was on in an empty space, then trigger the callback.
        onEmptyViewLongClickListener?.let { listener ->
            if (e.x > timeColumnWidth && e.y > config.headerHeight) {
                val selectedTime = touchHandler.calculateTimeFromPoint(e.x, e.y) ?: return@let
                listener.onEmptyViewLongClick(selectedTime)
            }
        }


    }

    internal fun findHitEvent(x: Float, y: Float): EventChip<T>? {
        val candidates = chipCache.allEventChips.filter { it.isHit(x, y) }
        return when {
            candidates.isEmpty() -> null
            // Two events hit. This is most likely because an all-day event was clicked, but a
            // single event is rendered underneath it. We return the all-day event.
            candidates.size == 2 -> candidates.first { it.event.isAllDay }
            else -> candidates.first()
        }
    }

    private fun goToNearestOrigin() {
        val totalDayWidth = config.totalDayWidth
        val leftDays = (config.currentOrigin.x / totalDayWidth).toDouble()

        val finalLeftDays = when {
            // snap to nearest day
            currentFlingDirection.isNotNone -> round(leftDays)
            // snap to last day
            currentScrollDirection.isLeft -> floor(leftDays)
            // snap to next day
            currentScrollDirection.isRight -> ceil(leftDays)
            // snap to nearest day
            else -> round(leftDays)
        }

        val nearestOrigin = (config.currentOrigin.x - finalLeftDays * totalDayWidth).toInt()

        if (nearestOrigin != 0) {
            // Stop current animation
            scroller.forceFinished(true)

            // Snap to date
            val startX = config.currentOrigin.x.toInt()
            val startY = config.currentOrigin.y.toInt()

            val distanceX = -nearestOrigin
            val distanceY = 0

            val daysScrolled = abs(nearestOrigin) / config.widthPerDay
            val duration = (daysScrolled * config.scrollDuration).toInt()

            scroller.startScroll(startX, startY, distanceX, distanceY, duration)
            listener.requireInvalidation()
        }

        // Reset scrolling and fling direction.
        currentFlingDirection = NONE
        currentScrollDirection = currentFlingDirection
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        val value = gestureDetector.onTouchEvent(event)

        // Check after call of gestureDetector, so currentFlingDirection and currentScrollDirection
        // are set
        if (event.action == ACTION_UP && !isZooming && currentFlingDirection == NONE) {
            if (currentScrollDirection == RIGHT || currentScrollDirection == LEFT) {
                goToNearestOrigin()
            }
            currentScrollDirection = NONE
        }

        if (isDragEnabled && event.getAction() == MotionEvent.ACTION_UP) {
            if (isDraggingGoingOn) {
                eventDragOverListener?.onDragOver(drag_start_time, drag_end_time);
            }
            dragOver();

        }

        if (isDragEnabled && event.getAction() == MotionEvent.ACTION_MOVE) {
            //Check if user is actually longpressing, not slow-moving
            // if current position differs much then press positon then discard whole thing
            // If position change is minimal then after 0.5s that is a longpress. You can now process your other gestures

            if (isDraggingGoingOn) {
                //drag and show view

                if (!(event.x > config.timeColumnWidth && event.getY() > config.headerHeight))
                    return value;


                var selectedCal = touchHandler.calculateTimeFromPoint(event.x, event.y);



                if (selectedCal != null) {


                    if (!dragStartTimeSet) {
                        drag_init_start_time = Calendar.getInstance();
                        drag_init_end_time = Calendar.getInstance();
                        drag_init_start_time.setTimeInMillis(roundOffTime(selectedCal, true).getTimeInMillis());
                        drag_init_end_time.setTimeInMillis(roundOffTime(selectedCal, false).getTimeInMillis());
                        dragStartTimeSet = true;
                    }

                    if (isCloseToQuarter(drag_init_start_time, drag_init_end_time, selectedCal)) {
                        callDrag(drag_init_start_time, drag_init_end_time);
                    } else {

                        if (selectedCal.getTimeInMillis() > drag_init_end_time.getTimeInMillis()) {
                            var updated_end_time = roundOffTime(selectedCal, false);
                            callDrag(drag_init_start_time, updated_end_time);
                        } else {
                            var updated_start_time = roundOffTime(selectedCal, true);
                            callDrag(updated_start_time, drag_init_end_time);
                        }


                    }


                }


//                scrollWhileDrag(event);

            }
        }
        return value
    }


    fun forceScrollFinished() {
        scroller.forceFinished(true)
        currentFlingDirection = NONE
        currentScrollDirection = currentFlingDirection
    }

    fun computeScroll() {
        val isFinished = scroller.isFinished
        val isFlinging = currentFlingDirection.isNotNone
        val isScrolling = currentScrollDirection.isNotNone

        if (isFinished && isFlinging) {
            // Snap to day after fling is finished
            goToNearestOrigin()
        } else if (isFinished and !isScrolling) {
            // Snap to day after scrolling is finished
            goToNearestOrigin()
        } else {
            if (isFlinging && shouldForceFinishScroll()) {
                goToNearestOrigin()
            } else if (scroller.computeScrollOffset()) {
                config.currentOrigin.y = scroller.currY.toFloat()
                config.currentOrigin.x = scroller.currX.toFloat()
                listener.requireInvalidation()
            }
        }
    }

    private fun shouldForceFinishScroll(): Boolean {
        return scroller.currVelocity <= minimumFlingVelocity
    }

    private val MotionEvent.isInHeader: Boolean
        get() = y in view.x..config.headerHeight

    private val View.scaledMinimumFlingVelocity: Int
        get() = ViewConfiguration.get(context).scaledMinimumFlingVelocity

    private val View.scaledTouchSlop: Int
        get() = ViewConfiguration.get(context).scaledTouchSlop


    fun callDrag(start_cal: Calendar, end_cal: Calendar) {

        if ((!::drag_start_time.isInitialized && !::drag_end_time.isInitialized) || (drag_start_time.timeInMillis != start_cal.timeInMillis || drag_end_time.timeInMillis != end_cal.timeInMillis)) {
            drag_start_time = start_cal
            drag_end_time = end_cal
            eventDraggingListener?.onDragging(drag_start_time, drag_end_time)
        }


    }

    private fun isCloseToQuarter(start_cal: Calendar, end_cal: Calendar, selected_cal: Calendar): Boolean {
        return selected_cal.timeInMillis >= start_cal.timeInMillis && selected_cal.timeInMillis < end_cal.timeInMillis
    }

    private fun dragOver() {

        isDraggingGoingOn = false
        dragStartTimeSet = false

    }


    fun roundOffTime(cal: Calendar, getNearestToStart: Boolean): Calendar {
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)


        val start_range = cal.get(Calendar.MINUTE) / snapMinutes * snapMinutes
        val end_range = start_range + snapMinutes

        if (getNearestToStart)
            cal.set(Calendar.MINUTE, start_range)
        else
            cal.set(Calendar.MINUTE, end_range)

        return cal
    }


    private fun setDragStatus() {
        isDragEnabled = eventDragBeginListener != null || eventDraggingListener != null || eventDragOverListener != null
    }


    internal interface Listener {
        fun requireInvalidation()
    }
}
