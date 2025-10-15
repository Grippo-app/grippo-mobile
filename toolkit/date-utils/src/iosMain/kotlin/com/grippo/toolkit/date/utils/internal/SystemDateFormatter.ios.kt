package com.grippo.toolkit.date.utils.internal.platform

import com.grippo.toolkit.date.utils.internal.DateFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.localTimeZone

internal actual class SystemDateFormatter actual constructor() : DateFormatter {
    actual override fun format(value: LocalDateTime, pattern: String, localeTag: String?): String? {
        val formatter = NSDateFormatter().apply {
            // Safer locale construction
            locale = localeTag?.let { NSLocale(localeIdentifier = it) } ?: NSLocale.currentLocale
            // calendarWithIdentifier returns NSCalendar? -> provide fallback
            calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)
                ?: NSCalendar.currentCalendar
            timeZone = NSTimeZone.localTimeZone
            dateFormat = normalizeApplePattern(pattern) // supports MMM/MMMM and LLL/LLLL
        }
        return value.toNSDate()?.let { formatter.stringFromDate(it) }
    }

    actual override fun format(
        value: LocalDate,
        pattern: String,
        localeTag: String?
    ): String? {
        val formatter = NSDateFormatter().apply {
            locale = localeTag?.let { NSLocale(localeIdentifier = it) } ?: NSLocale.currentLocale
            calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)
                ?: NSCalendar.currentCalendar
            timeZone = NSTimeZone.localTimeZone
            dateFormat = normalizeApplePattern(pattern)
        }
        return value.toNSDateAtNoon()?.let { formatter.stringFromDate(it) }
    }

    actual override fun format(value: LocalTime, pattern: String, localeTag: String?): String? {
        val formatter = NSDateFormatter().apply {
            locale = localeTag?.let { NSLocale(localeIdentifier = it) } ?: NSLocale.currentLocale
            calendar = NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)
                ?: NSCalendar.currentCalendar
            timeZone = NSTimeZone.localTimeZone
            dateFormat = normalizeApplePattern(pattern)
        }
        return value.toNSDateOnReferenceDate()?.let { formatter.stringFromDate(it) }
    }
}

private fun LocalDate.toNSDateAtNoon(): NSDate? {
    // Use noon to avoid DST edge cases when midnight may jump
    val comps = NSDateComponents().apply {
        year = this@toNSDateAtNoon.year.toLong()
        month = this@toNSDateAtNoon.monthNumber.toLong()
        day = this@toNSDateAtNoon.dayOfMonth.toLong()
        hour = 12
        minute = 0
        second = 0
        timeZone = NSTimeZone.localTimeZone
    }
    return NSCalendar.currentCalendar.dateFromComponents(comps)
}

private fun LocalTime.toNSDateOnReferenceDate(): NSDate? {
    // Fixed reference date to format time-only values
    val comps = NSDateComponents().apply {
        year = 2001 // Apple reference date
        month = 1
        day = 1
        hour = this@toNSDateOnReferenceDate.hour.toLong()
        minute = this@toNSDateOnReferenceDate.minute.toLong()
        second = this@toNSDateOnReferenceDate.second.toLong()
        timeZone = NSTimeZone.localTimeZone
    }
    return NSCalendar.currentCalendar.dateFromComponents(comps)
}

private fun LocalDateTime.toNSDate(): NSDate? {
    val comps = NSDateComponents().apply {
        year = this@toNSDate.year.toLong()
        month = this@toNSDate.monthNumber.toLong()
        day = this@toNSDate.dayOfMonth.toLong()
        hour = this@toNSDate.hour.toLong()
        minute = this@toNSDate.minute.toLong()
        second = this@toNSDate.second.toLong()
    }
    return NSCalendar.currentCalendar.dateFromComponents(comps)
}

// Normalize Java-style 'u' to Apple 'y'
private fun normalizeApplePattern(pattern: String): String = pattern.replace('u', 'y')