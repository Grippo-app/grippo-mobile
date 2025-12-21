package com.grippo.toolkit.date.utils

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

public object DateTimeUtils {

    private val timeZone: TimeZone = TimeZone.currentSystemDefault()

    public fun now(): LocalDateTime {
        return Clock.System.now().toLocalDateTime(timeZone)
    }

    /* * * * * * * * * * *
     * Instant
     * * * * * * * * * * */

    public fun asInstant(value: LocalDateTime): Instant {
        return value.toInstant(timeZone)
    }

    /* * * * * * * * * * *
     * Date Range
     * * * * * * * * * * */

    public fun thisDay(): DateRange {
        val date = Clock.System.now().toLocalDateTime(timeZone).date

        return DateRange(
            from = date.atTime(DayTime.StartOfDay.localTime),
            to = date.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun thisWeek(): DateRange {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val daysToStartOfWeek = today.dayOfWeek.ordinal
        val startOfWeek = today.minus(DatePeriod(days = daysToStartOfWeek))
        val daysToEndOfWeek = DayOfWeek.SUNDAY.ordinal - today.dayOfWeek.ordinal
        val endOfWeek = today.plus(DatePeriod(days = daysToEndOfWeek))

        return DateRange(
            from = startOfWeek.atTime(DayTime.StartOfDay.localTime),
            to = endOfWeek.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun thisMonth(): DateRange {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val startOfMonth = LocalDate(today.year, today.month, 1)
        val lastDayOfMonth = startOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

        return DateRange(
            from = startOfMonth.atTime(DayTime.StartOfDay.localTime),
            to = lastDayOfMonth.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun thisYear(): DateRange {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        // Start at Jan 1, end at Dec 31 (via +1 year -1 day to handle leap years safely)
        val startOfYear = LocalDate(today.year, Month.JANUARY, 1)
        val endOfYear = startOfYear.plus(DatePeriod(years = 1)).minus(DatePeriod(days = 1))

        return DateRange(
            from = startOfYear.atTime(DayTime.StartOfDay.localTime),
            to = endOfYear.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun trailingYear(): DateRange {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val oneYearAgo = today.minus(DatePeriod(years = 1))

        return DateRange(
            from = oneYearAgo.atTime(DayTime.StartOfDay.localTime),
            to = today.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun trailingMonth(): DateRange {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val monthAgo = today.minus(DatePeriod(days = 31))

        return DateRange(
            from = monthAgo.atTime(DayTime.StartOfDay.localTime),
            to = today.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun trailingWeek(): DateRange {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val weekAgo = today.minus(DatePeriod(days = 7))

        return DateRange(
            from = weekAgo.atTime(DayTime.StartOfDay.localTime),
            to = today.atTime(DayTime.EndOfDay.localTime)
        )
    }

    /* * * * * * * * * * *
     * Time for date
     * * * * * * * * * * */

    public fun startOfDay(value: LocalDateTime): LocalDateTime {
        return value.date.atTime(DayTime.StartOfDay.localTime)
    }

    public fun endOfDay(value: LocalDateTime): LocalDateTime {
        return value.date.atTime(DayTime.EndOfDay.localTime)
    }

    public fun startOfDay(value: LocalDate): LocalDateTime {
        return value.atTime(DayTime.StartOfDay.localTime)
    }

    public fun endOfDay(value: LocalDate): LocalDateTime {
        return value.atTime(DayTime.EndOfDay.localTime)
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
        return DateFormatting.current.format(value, format.pattern, null) ?: "-"
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    public fun format(value: LocalTime, format: DateFormat.TimeOnly): String {
        return DateFormatting.current.format(value, format.pattern, null) ?: "-"
    }

    @OptIn(FormatStringsInDatetimeFormats::class)
    public fun format(value: LocalDate, format: DateFormat.DateOnly): String {
        return DateFormatting.current.format(value, format.pattern, null) ?: "-"
    }

    public fun format(duration: Duration): String {
        return DateFormatting.current.format(duration) ?: "-"
    }
    /* * * * * * * * * * *
     * Calculation
     * * * * * * * * * * */

    public fun ago(value: LocalDateTime): Duration {
        val now = Clock.System.now()
        val fromInstant = value.toInstant(timeZone)
        return now - fromInstant
    }

    public fun minus(value: LocalDateTime, minus: Duration): LocalDateTime {
        val instant = value.toInstant(timeZone) - minus
        return instant.toLocalDateTime(timeZone)
    }

    public fun plus(value: LocalDateTime, plus: Duration): LocalDateTime {
        val instant = value.toInstant(timeZone) + plus
        return instant.toLocalDateTime(timeZone)
    }

    public fun shift(range: DateRange, period: DatePeriod): DateRange {
        val newFromDate = range.from.date.plus(period)
        val newToDate = range.to.date.plus(period)
        return DateRange(
            from = newFromDate.atTime(range.from.time),
            to = newToDate.atTime(range.to.time)
        )
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

    /* * * * * * * * * * *
    * Static
    * * * * * * * * * * */

    public fun getDaysInMonth(year: Int, month: Month): Int {
        val nextMonth =
            if (month == Month.DECEMBER) Month.JANUARY else Month.entries[month.ordinal + 1]
        val nextYear = if (month == Month.DECEMBER) year + 1 else year
        val firstOfNextMonth = LocalDate(nextYear, nextMonth, 1)
        val lastDayOfThisMonth = firstOfNextMonth.minus(DatePeriod(days = 1))
        return lastDayOfThisMonth.day
    }
}
