package com.grippo.toolkit.date.utils

import com.grippo.toolkit.date.utils.internal.DayTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.isoDayNumber
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

    public fun today(): LocalDate {
        return Clock.System.todayIn(timeZone)
    }

    public fun asInstant(value: LocalDateTime): Instant {
        return value.toInstant(timeZone)
    }

    public fun thisDay(anchor: LocalDate = today()): DateRange {
        return DateRange(
            from = anchor.atTime(DayTime.StartOfDay.localTime),
            to = anchor.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun leadingYear(): DateRange {
        val date = Clock.System.now().toLocalDateTime(timeZone).date

        return DateRange(
            from = date.atTime(DayTime.StartOfDay.localTime),
            to = date.plus(DatePeriod(years = 1)).atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun thisWeek(anchor: LocalDate = today()): DateRange {
        val startOfWeek = startOfIsoWeek(anchor)
        val endOfWeek = startOfWeek.plus(DatePeriod(days = 6))

        return DateRange(
            from = startOfWeek.atTime(DayTime.StartOfDay.localTime),
            to = endOfWeek.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun thisMonth(anchor: LocalDate = today()): DateRange {
        val startOfMonth = LocalDate(anchor.year, anchor.month, 1)
        val lastDayOfMonth = startOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))

        return DateRange(
            from = startOfMonth.atTime(DayTime.StartOfDay.localTime),
            to = lastDayOfMonth.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun thisYear(anchor: LocalDate = today()): DateRange {
        val startOfYear = LocalDate(anchor.year, Month.JANUARY, 1)
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

    public fun trailingMonth(anchor: LocalDate = today()): DateRange {
        return trailingDays(days = 30, anchor = anchor)
    }

    public fun trailing14Days(anchor: LocalDate = today()): DateRange {
        return trailingDays(days = 14, anchor = anchor)
    }

    public fun trailing60Days(anchor: LocalDate = today()): DateRange {
        return trailingDays(days = 60, anchor = anchor)
    }

    public fun trailingWeek(anchor: LocalDate = today()): DateRange {
        return trailingDays(days = 7, anchor = anchor)
    }

    internal fun trailingDays(days: Int, anchor: LocalDate = today()): DateRange {
        require(days > 0)

        val daysAgo = anchor.minus(DatePeriod(days = days - 1))

        return DateRange(
            from = daysAgo.atTime(DayTime.StartOfDay.localTime),
            to = anchor.atTime(DayTime.EndOfDay.localTime)
        )
    }

    public fun infinity(): DateRange {
        return DateRange(
            from = LocalDateTime(1970, 1, 1, 0, 0, 0, 0),
            to = LocalDateTime(9999, 12, 31, 23, 59, 59, 999_999_900),
        )
    }

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

    public fun startOfIsoWeek(value: LocalDate): LocalDate {
        return value.minus(DatePeriod(days = value.dayOfWeek.isoDayNumber - 1))
    }

    public fun toUtcIso(value: LocalDateTime): String {
        return value.toInstant(timeZone).toString()
    }

    public fun toLocalDateTime(timestamp: String): LocalDateTime {
        return Instant
            .parse(timestamp)
            .toLocalDateTime(timeZone)
    }

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

    public fun isToday(date: LocalDate): Boolean {
        val today = Clock.System.todayIn(timeZone)
        return date == today
    }

    public fun isPast(date: LocalDate): Boolean {
        val today = Clock.System.todayIn(timeZone)
        return date < today
    }

    public fun isPast(value: LocalDateTime): Boolean {
        return value.toInstant(timeZone) < Clock.System.now()
    }

    public fun getDaysInMonth(year: Int, month: Month): Int {
        val nextMonth =
            if (month == Month.DECEMBER) Month.JANUARY else Month.entries[month.ordinal + 1]
        val nextYear = if (month == Month.DECEMBER) year + 1 else year
        val firstOfNextMonth = LocalDate(nextYear, nextMonth, 1)
        val lastDayOfThisMonth = firstOfNextMonth.minus(DatePeriod(days = 1))
        return lastDayOfThisMonth.day
    }
}
