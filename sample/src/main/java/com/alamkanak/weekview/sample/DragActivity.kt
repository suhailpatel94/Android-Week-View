package com.alamkanak.weekview.sample

import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.alamkanak.weekview.*
import com.alamkanak.weekview.sample.data.EventsDatabase
import com.alamkanak.weekview.sample.data.model.Event
import com.alamkanak.weekview.sample.util.lazyView
import com.alamkanak.weekview.sample.util.setupWithWeekView
import com.alamkanak.weekview.sample.util.showToast
import kotlinx.android.synthetic.main.activity_drag.*
import kotlinx.android.synthetic.main.view_toolbar.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


class DragActivity : AppCompatActivity(), OnEventClickListener<Event>,
        OnMonthChangeListener<Event>, EventDragBeginListener, EventDraggingListener, EventDragOverListener {


    private val weekView: WeekView<Event> by lazyView(R.id.weekView)

    private val database: EventsDatabase by lazy { EventsDatabase(this) }

    var dragEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drag)

        toolbar.setupWithWeekView(weekView)

        weekView.onEventClickListener = this
        weekView.onMonthChangeListener = this
        weekView.eventDragBeginListener = this
        weekView.eventDraggingListener = this
        weekView.eventDragOverListener = this


    }

    override fun onMonthChange(startDate: Calendar, endDate: Calendar): List<WeekViewDisplayable<Event>> {

        var eventList: MutableList<WeekViewDisplayable<Event>> = mutableListOf()
        eventList.addAll(database.getEventsInRange(startDate, endDate))

        dragEvent?.let {
            eventList.add(it)
        }
        return eventList
    }

    override fun onEventClick(event: Event, eventRect: RectF) {
        showToast("Clicked ${event.title}")
    }


    override fun onDragBegin() {

    }

    override fun onDragging(startDateTime: Calendar, endDateTime: Calendar) {
        dragEvent = Event(Random().nextInt(1000).toLong(), "", startDateTime, endDateTime, "", Color.GRAY, false, false)
        weekView.notifyDataSetChanged()
    }

    override fun onDragOver(startDateTime: Calendar, endDateTime: Calendar) {
        showAlert()
    }

    fun showAlert() {
        val builder = AlertDialog.Builder(this)
        //set title for alert dialog
        builder.setTitle("Event Title")
        //set message for alert dialog
        //performing positive action
        val view = layoutInflater.inflate(R.layout.item_title, null, false)

        builder.setView(view)
        val title_et: EditText = view.findViewById(R.id.title)
        builder.setPositiveButton("Yes") { dialogInterface, which ->
            dragEvent?.title = title_et.text.toString()
            weekView.notifyDataSetChanged()
        }
        //performing cancel action
        builder.setNegativeButton("Cancel") { dialogInterface, which ->
            weekView.notifyDataSetChanged()
        }

        // Create the AlertDialog
        val alertDialog: AlertDialog = builder.create()
        // Set other dialog properties
        alertDialog.setCancelable(false)
        alertDialog.show()
    }
}
