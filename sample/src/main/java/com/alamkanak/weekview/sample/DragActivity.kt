package com.alamkanak.weekview.sample

import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Preconditions.checkNotNull
import com.alamkanak.weekview.*
import com.alamkanak.weekview.sample.data.EventsDatabase
import com.alamkanak.weekview.sample.data.model.Event
import com.alamkanak.weekview.sample.util.lazyView
import com.alamkanak.weekview.sample.util.setupWithWeekView
import com.alamkanak.weekview.sample.util.showToast
import kotlinx.android.synthetic.main.activity_static.*
import kotlinx.android.synthetic.main.view_toolbar.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class DragActivity : AppCompatActivity(), OnEventClickListener<Event>,
        OnMonthChangeListener<Event>, OnEventLongClickListener<Event>, OnEmptyViewLongClickListener {

    private val weekView: WeekView<Event> by lazyView(R.id.weekView)

    private val database: EventsDatabase by lazy { EventsDatabase(this) }
    private val dateFormatter = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM)
    var last_touch_x: Float = 0.0f
    var last_touch_y: Float = 0.0f
    lateinit var dragUtil: DragUtil
    lateinit var parent: CustomSV
    lateinit var drag_container: FrameLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drag)
        parent = findViewById(R.id.parent)
        drag_container = findViewById(R.id.drag_container)

        toolbar.setupWithWeekView(weekView)

        weekView.onEventClickListener = this
        weekView.onMonthChangeListener = this
        weekView.onEventLongClickListener = this
        weekView.onEmptyViewLongClickListener = this

        previousWeekButton.setOnClickListener {
            val cal = checkNotNull(weekView.firstVisibleDate)
            cal.add(Calendar.DATE, -7)
            weekView.goToDate(cal)
        }

        nextWeekButton.setOnClickListener {
            val cal = checkNotNull(weekView.firstVisibleDate)
            cal.add(Calendar.DATE, 7)
            weekView.goToDate(cal)
        }

        weekView.onRangeChangeListener = object : OnRangeChangeListener {
            override fun onRangeChanged(
                    firstVisibleDate: Calendar,
                    lastVisibleDate: Calendar
            ) = updateDateText(firstVisibleDate, lastVisibleDate)
        }


        weekView.setOnTouchListener { v, event ->
            last_touch_x = event.x
            last_touch_y = event.y
            false
        }

        dragUtil = DragUtil(parent, drag_container, weekView, this)


        dragUtil.setOnDragCompleted(DragUtil.DragCompleted { start, end ->

            val format1 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

            val formatted1: String = format1.format(start.time)
            val formatted2: String = format1.format(end.time)
            Log.e("DATTM_1", formatted1)
            Log.e("DATTM_2", formatted2)

        })

        dragUtil.init()

    }

    override fun onMonthChange(
            startDate: Calendar,
            endDate: Calendar
    ) = database.getEventsInRange(startDate, endDate)

    override fun onEventClick(event: Event, eventRect: RectF) {
        showToast("Clicked ${event.title}")
    }

    override fun onEventLongClick(event: Event, eventRect: RectF) {
        showToast("Long-clicked ${event.title}")

        Toast.makeText(this, "Long pressed event: " + event.title, Toast.LENGTH_SHORT).show()
    }

    override fun onEmptyViewLongClick(time: Calendar) {
        val sdf = SimpleDateFormat.getDateTimeInstance()
        weekView.getSnappedPixel(last_touch_x, last_touch_y, true)
        weekView.calculateTimeFromPoint(last_touch_x, last_touch_y)
        Log.e("TOUCHPX", "$last_touch_x || $last_touch_y")
        showToast("Empty view long-clicked at ${sdf.format(time.time)}")
    }

    internal fun updateDateText(firstVisibleDate: Calendar, lastVisibleDate: Calendar) {
        val formattedFirstDay = dateFormatter.format(firstVisibleDate.time)
        val formattedLastDay = dateFormatter.format(lastVisibleDate.time)
        dateRangeTextView.text = getString(R.string.date_infos, formattedFirstDay, formattedLastDay)
    }

}
