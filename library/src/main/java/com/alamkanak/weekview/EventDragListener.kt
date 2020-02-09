package com.alamkanak.weekview

import java.util.*

@FunctionalInterface
interface EventDragListener<T> {

    fun onDragBegin()
    fun onDragging(cal_start: Calendar, cal_end: Calendar)
    fun onDragOver(cal_start: Calendar, cal_end: Calendar)

}
