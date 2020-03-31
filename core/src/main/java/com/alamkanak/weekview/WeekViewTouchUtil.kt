package com.alamkanak.weekview

import java.util.*
import kotlin.math.ceil
import kotlin.math.max

internal class WeekViewTouchUtil(
        private val config: WeekViewConfigWrapper
) {

    fun calculateTimeFromPoint(
            touchX: Float,
            touchY: Float
    ): Calendar? {
        val widthPerDay = config.widthPerDay
        val totalDayWidth = widthPerDay + config.columnGap
        val originX = config.currentOrigin.x
        val timeColumnWidth = config.timeColumnWidth

        val daysFromOrigin = (ceil((originX / totalDayWidth).toDouble()) * -1).toInt()
        var startPixel = originX + daysFromOrigin * totalDayWidth + timeColumnWidth

        val firstDay = daysFromOrigin + 1
        val lastDay = firstDay + config.numberOfVisibleDays

        for (dayNumber in firstDay..lastDay) {
            val start = max(startPixel, timeColumnWidth)
            val end = startPixel + totalDayWidth
            val width = end - start

            val isVisibleHorizontally = width > 0
            val isWithinDay = touchX in start..end

            if (isVisibleHorizontally && isWithinDay) {
                val day = now() + Days(dayNumber - 1)

                val hourHeight = config.hourHeight
                val pixelsFromMidnight = touchY - config.currentOrigin.y - config.headerHeight
                val hour = (pixelsFromMidnight / hourHeight).toInt()
                val pixelsFromFullHour = pixelsFromMidnight - hour * hourHeight
                val minutes = ((pixelsFromFullHour / hourHeight) * 60).toInt()

                return day.withTime(config.minHour + hour, minutes)
            }

            startPixel += totalDayWidth
        }

        return null
    }

    fun getTopLimit(): Float {
        return config.currentOrigin.y + config.headerHeight
    }

    fun getYPixelFromMinutes(
            touch_minutes: Long
    ): Float {

        val minHeightpx = config.hourHeight / 60
        val tounMinPx = minHeightpx * touch_minutes

        return tounMinPx + config.currentOrigin.y + config.headerHeight
    }

    fun getSnappedPixel(
            touchX: Float,
            touchY: Float,
            snapToTop: Boolean
    ): PointCalendarWrapper? {
        val widthPerDay = config.widthPerDay
        val totalDayWidth = widthPerDay + config.columnGap
        val originX = config.currentOrigin.x
        val timeColumnWidth = config.timeColumnWidth

        val daysFromOrigin = (ceil((originX / totalDayWidth).toDouble()) * -1).toInt()
        var startPixel = originX + daysFromOrigin * totalDayWidth + timeColumnWidth

        val firstDay = daysFromOrigin + 1
        val lastDay = firstDay + config.numberOfVisibleDays


        for (dayNumber in firstDay..lastDay) {
            val start = max(startPixel, timeColumnWidth)
            val end = startPixel + totalDayWidth
            val width = end - start

            val isVisibleHorizontally = width > 0
            val isWithinDay = touchX in start..end

            if (isVisibleHorizontally && isWithinDay) {

                val day = now() + Days(dayNumber - 1)
                val sections_per_hour = 4
                val minutes_per_section: Int = 60 / sections_per_hour;
                val hourHeight = config.hourHeight
                val sectionHeight: Float = config.hourHeight / sections_per_hour
                val pixelsFromMidnight = touchY - config.currentOrigin.y - config.headerHeight
                val hour = (pixelsFromMidnight / hourHeight).toInt()

                val hour_pixel_from_midnight = hour * hourHeight
                val pixelsFromFullHour = pixelsFromMidnight - hour_pixel_from_midnight
                val current_section_no: Int = (pixelsFromFullHour / sectionHeight).toInt() + 1
                val current_section_top_y_FromMidNight = hour_pixel_from_midnight + (current_section_no * sectionHeight) - sectionHeight
                val snapped_pixel: Float
                if (snapToTop)
                    snapped_pixel = current_section_top_y_FromMidNight;
                else
                    snapped_pixel = current_section_top_y_FromMidNight + sectionHeight
                val current_section_minutes: Int = current_section_no * minutes_per_section
                val minutes: Int
                if (snapToTop)
                    minutes = current_section_minutes - minutes_per_section
                else
                    minutes = current_section_minutes

                val cal: Calendar = Calendar.getInstance()
                cal.timeInMillis = day.timeInMillis
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.set(Calendar.HOUR_OF_DAY, config.minHour + hour)
                cal.set(Calendar.MINUTE, 0)

                cal.add(Calendar.MINUTE, minutes)

                return PointCalendarWrapper(cal, start, snapped_pixel + config.currentOrigin.y + config.headerHeight, totalDayWidth.toInt())

            }

            startPixel += totalDayWidth
        }

        return null
    }


}
