package com.alamkanak.weekview

import java.util.Calendar

@FunctionalInterface
interface OnEmptyViewLongPressListener {

    /**
     * Called when an empty area of [WeekView] is long-clicked.
     *
     * @param time A [Calendar] with the date and time of the clicked position
     */
    fun onEmptyViewLongPress(time: Calendar)

}
