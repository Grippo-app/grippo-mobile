package com.grippo.toolkit.date.utils.internal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateComponentsFormatter
import platform.Foundation.NSDateComponentsFormatterUnitsStyleShort
import platform.Foundation.NSDateComponentsFormatterZeroFormattingBehaviorDropAll
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import kotlin.time.Duration

internal actual class SystemDateFormatter actual constructor() {
    actual fun format(value: LocalDateTime, pattern: String, localeTag: String): String? {
        val formatter = NSDateFormatter().apply {
            locale = NSLocale(localeIdentifier = localeTag)
            calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)
                ?: NSCalendar.currentCalendar
            timeZone = NSTimeZone.localTimeZone
            dateFormat = normalizeApplePattern(pattern)
        }
        return value.toNSDate()?.let { formatter.stringFromDate(it) }
    }

    actual fun format(
        value: LocalDate,
        pattern: String,
        localeTag: String
    ): String? {
        val formatter = NSDateFormatter().apply {
            locale = NSLocale(localeIdentifier = localeTag)
            calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)
                ?: NSCalendar.currentCalendar
            timeZone = NSTimeZone.localTimeZone
            dateFormat = normalizeApplePattern(pattern)
        }
        return value.toNSDateAtNoon()?.let { formatter.stringFromDate(it) }
    }

    actual fun format(duration: Duration, localeTag: String): String? {
        val isNegative = duration.isNegative()
        val absoluteSeconds = duration.absoluteValue.inWholeSeconds
        val localeValue = NSLocale(localeIdentifier = localeTag)

        val formatter = NSDateComponentsFormatter().apply {
            allowedUnits = NSCalendarUnitHour or NSCalendarUnitMinute
            unitsStyle = NSDateComponentsFormatterUnitsStyleShort
            zeroFormattingBehavior = NSDateComponentsFormatterZeroFormattingBehaviorDropAll
            val localCalendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)
                ?: NSCalendar.currentCalendar
            localCalendar.locale = localeValue
            localCalendar.timeZone = NSTimeZone.localTimeZone
            calendar = localCalendar
        }

        val formatted = formatter.stringFromTimeInterval(absoluteSeconds.toDouble()) ?: return null
        return if (isNegative) "-$formatted" else formatted
    }
}

private fun LocalDate.toNSDateAtNoon(): NSDate? {
    // Use noon to avoid DST edge cases when midnight may jump
    val comps = NSDateComponents().apply {
        year = this@toNSDateAtNoon.year.toLong()
        month = this@toNSDateAtNoon.month.number.toLong()
        day = this@toNSDateAtNoon.day.toLong()
        hour = 12
        minute = 0
        second = 0
        timeZone = NSTimeZone.localTimeZone
    }
    return NSCalendar.currentCalendar.dateFromComponents(comps)
}

private fun LocalDateTime.toNSDate(): NSDate? {
    val comps = NSDateComponents().apply {
        year = this@toNSDate.year.toLong()
        month = this@toNSDate.month.number.toLong()
        day = this@toNSDate.day.toLong()
        hour = this@toNSDate.hour.toLong()
        minute = this@toNSDate.minute.toLong()
        second = this@toNSDate.second.toLong()
    }
    return NSCalendar.currentCalendar.dateFromComponents(comps)
}

private fun normalizeApplePattern(pattern: String): String = pattern.replace('u', 'y')
