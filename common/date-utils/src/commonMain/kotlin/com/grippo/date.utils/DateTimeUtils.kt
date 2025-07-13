package com.grippo.date.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Duration

public object DateTimeUtils {

    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

    public fun thisDay(): LocalDateTime {
        return Clock.System.now().toLocalDateTime(timeZone)
    }

    public fun thisWeek(): Pair<LocalDate, LocalDate> {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val daysToStartOfWeek = today.dayOfWeek.ordinal
        val startOfWeek = today.minus(DatePeriod(days = daysToStartOfWeek))
        val daysToEndOfWeek = DayOfWeek.SUNDAY.ordinal - today.dayOfWeek.ordinal
        val endOfWeek = today.plus(DatePeriod(days = daysToEndOfWeek))
        return Pair(startOfWeek, endOfWeek)
    }

    public fun thisMonth(): Pair<LocalDate, LocalDate> {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val startOfMonth = LocalDate(today.year, today.month, 1)
        val lastDayOfMonth = startOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        return Pair(startOfMonth, lastDayOfMonth)
    }

    /* * * * * * * * * * *
     * Parsing via TimeZone
     * * * * * * * * * * */

    public fun toUtcIso(value: LocalDateTime): String {
        return value.toInstant(timeZone).toString()
    }

    public fun toLocalDateTime(timestamp: String): LocalDateTime {
        return Instant
            .parse(timestamp)
            .toLocalDateTime(timeZone)
    }

    /* * * * * * * * * * *
     * Date/Time Formatting
     * * * * * * * * * * */

    @OptIn(FormatStringsInDatetimeFormats::class)
    public fun format(value: LocalDateTime, format: DateFormat): String {
        return value.format(LocalDateTime.Format { byUnicodePattern(format.value) })
    }

    public fun ago(value: LocalDateTime): Duration {
        val now = Clock.System.now()
        val fromInstant = value.toInstant(timeZone)
        return now - fromInstant
    }

    /* * * * * * * * * * *
     * Timeline
     * * * * * * * * * * */

    public fun isToday(date: LocalDate): Boolean {
        val today = Clock.System.todayIn(timeZone)
        return date == today
    }

    public fun isYesterday(date: LocalDate): Boolean {
        val yesterday = Clock.System
            .todayIn(timeZone)
            .minus(DatePeriod(days = 1))
        return date == yesterday
    }

    public fun isTomorrow(date: LocalDate): Boolean {
        val tomorrow = Clock.System
            .todayIn(timeZone)
            .plus(DatePeriod(days = 1))
        return date == tomorrow
    }

    public fun isFuture(date: LocalDate): Boolean {
        val today = Clock.System.todayIn(timeZone)
        return date > today
    }

    public fun isPast(date: LocalDate): Boolean {
        val today = Clock.System.todayIn(timeZone)
        return date < today
    }
}
