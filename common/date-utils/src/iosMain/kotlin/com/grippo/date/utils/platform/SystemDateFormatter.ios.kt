package com.grippo.date.utils.platform

import com.grippo.date.utils.DateFormatter
import kotlinx.datetime.LocalDateTime
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