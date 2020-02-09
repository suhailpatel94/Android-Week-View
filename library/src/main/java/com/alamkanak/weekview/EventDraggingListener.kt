package com.alamkanak.weekview

import java.util.*

@FunctionalInterface
interface EventDraggingListener<T> {

    fun onDragging(cal_start: Calendar, cal_end: Calendar)

}
