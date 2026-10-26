package com.grippo.toolkit.date.utils.internal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toNSTimeZone
import platform.Foundation.NSCache
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponentsFormatter
import platform.Foundation.NSDateComponentsFormatterUnitsStyleShort
import platform.Foundation.NSDateComponentsFormatterZeroFormattingBehaviorDropAll
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.Duration
import kotlin.time.Instant

internal actual class SystemDateFormatter actual constructor() {
    actual fun format(value: LocalDateTime, pattern: String, localeTag: String): String? {
        val instant = value.toInstant(TimeZone.UTC)
        return format(instant = instant, pattern = pattern, localeTag = localeTag)
    }

    actual fun format(
        value: LocalDate,
        pattern: String,
        localeTag: String
    ): String? {
        val instant = value.atStartOfDayIn(TimeZone.UTC)
        return format(instant = instant, pattern = pattern, localeTag = localeTag)
    }

    actual fun format(duration: Duration, localeTag: String): String? {
        val isNegative = duration.isNegative()
        val absoluteSeconds = duration.absoluteValue.inWholeSeconds
        val formatter = durationFormatter(localeTag) ?: return null

        val formatted = formatter.stringFromTimeInterval(absoluteSeconds.toDouble()) ?: return null
        return if (isNegative) "-$formatted" else formatted
    }

    private fun format(instant: Instant, pattern: String, localeTag: String): String? {
        val formatter = dateFormatter(pattern = pattern, localeTag = localeTag) ?: return null
        // Whole seconds: as a Double, the end-of-day 23:59:59.9999999 rounds up to the next day.
        val epochSeconds = instant.epochSeconds.toDouble()
        val date = NSDate.dateWithTimeIntervalSince1970(epochSeconds)
        return formatter.stringFromDate(date)
    }

    private companion object {
        private val dateFormatters = NSCache()
        private val durationFormatters = NSCache()

        private fun dateFormatter(pattern: String, localeTag: String): NSDateFormatter? {
            val key = "$localeTag $pattern"
            val cached = dateFormatters.objectForKey(key) as? NSDateFormatter
            if (cached != null) return cached

            val gregorian = gregorianCalendar(localeTag) ?: return null
            val formatter = NSDateFormatter().apply {
                locale = NSLocale(localeIdentifier = localeTag)
                calendar = gregorian
                timeZone = TimeZone.UTC.toNSTimeZone()
                dateFormat = normalizeApplePattern(pattern)
            }
            dateFormatters.setObject(obj = formatter, forKey = key)
            return formatter
        }

        private fun durationFormatter(localeTag: String): NSDateComponentsFormatter? {
            val cached = durationFormatters.objectForKey(localeTag) as? NSDateComponentsFormatter
            if (cached != null) return cached

            val gregorian = gregorianCalendar(localeTag) ?: return null
            val formatter = NSDateComponentsFormatter().apply {
                allowedUnits = NSCalendarUnitHour or NSCalendarUnitMinute
                unitsStyle = NSDateComponentsFormatterUnitsStyleShort
                zeroFormattingBehavior = NSDateComponentsFormatterZeroFormattingBehaviorDropAll
                calendar = gregorian
            }
            durationFormatters.setObject(obj = formatter, forKey = localeTag)
            return formatter
        }

        private fun gregorianCalendar(localeTag: String): NSCalendar? {
            return NSCalendar.calendarWithIdentifier(NSCalendarIdentifierGregorian)?.apply {
                locale = NSLocale(localeIdentifier = localeTag)
            }
        }
    }
}

private fun normalizeApplePattern(pattern: String): String = pattern.replace('u', 'y')
