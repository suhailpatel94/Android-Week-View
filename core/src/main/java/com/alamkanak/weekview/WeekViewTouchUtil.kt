package com.alamkanak.weekview

import android.util.Log
import java.util.Calendar
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

    fun getLeftAndRightPixelFromPoint(
            touchX: Float,
            touchY: Float
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

                val hourHeight = config.hourHeight
                val pixelsFromMidnight = touchY - config.currentOrigin.y - config.headerHeight
                val hour = (pixelsFromMidnight / hourHeight).toInt()

                val pixelsFromFullHour = pixelsFromMidnight - hour * hourHeight
                val minutes = ((pixelsFromFullHour / hourHeight) * 60).toInt()
                Log.e("RNDX", "XL = " + startPixel + " || XR = " + (startPixel + width));
                return PointCalendarWrapper(day.withTime(config.minHour + hour, minutes), startPixel, startPixel + width)
            }

            startPixel += totalDayWidth
        }

        return null
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
                Log.e("RNDX_HOUR_PX", "pixelsFromMidnight = " + pixelsFromMidnight + " || hourHeight = " + hourHeight + " || hour_px = " + pixelsFromMidnight + hourHeight);
                val pixelsFromFullHour = pixelsFromMidnight - hour * hourHeight
                val current_section: Int = (pixelsFromFullHour / sectionHeight).toInt() + 1
                val current_section_top_y = current_section * sectionHeight
                val snapped_pixel: Float
                if (snapToTop)
                    snapped_pixel = current_section_top_y;
                else
                    snapped_pixel = current_section_top_y + sectionHeight
                val quarter_minute: Int
                if (snapToTop)
                    quarter_minute = ((current_section_top_y / hourHeight) * 60).toInt() - minutes_per_section
                else
                    quarter_minute = ((current_section_top_y / hourHeight) * 60).toInt()

                Log.e("RNDX", "${day.withTime(config.minHour + hour, quarter_minute)}")

                return PointCalendarWrapper(day.withTime(config.minHour + hour, quarter_minute), touchX, snapped_pixel)

            }

            startPixel += totalDayWidth
        }

        return null
    }


}
