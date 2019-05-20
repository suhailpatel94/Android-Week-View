package com.alamkanak.weekview

import java.util.*

@FunctionalInterface
interface EventDragListener<T> {


    fun onDragStart(data: Calendar)
    fun onDragging(data: Calendar, data1: Calendar)
    fun onDragOver()

}
