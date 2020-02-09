package com.alamkanak.weekview

import java.util.*

@FunctionalInterface
interface EventDragOverListener<T> {

    fun onDragOver(start_time:Calendar,end_time:Calendar)

}
