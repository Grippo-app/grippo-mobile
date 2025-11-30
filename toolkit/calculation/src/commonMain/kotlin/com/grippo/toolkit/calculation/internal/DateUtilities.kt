package com.grippo.toolkit.calculation.internal

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number

internal fun isoWeekNumber(weekStartMonday: LocalDateTime): Int {
    val date = weekStartMonday.date
    val firstJan = LocalDate(date.year, 1, 1)
    val doy = (date.toEpochDays() - firstJan.toEpochDays()).toInt() + 1
    return ((doy - 1) / 7) + 1
}

internal fun daysInclusive(from: LocalDate, to: LocalDate): Int {
    // O(1): inclusive day count
    return (to.toEpochDays() - from.toEpochDays() + 1).toInt()
}

internal fun startOfMonth(dateTime: LocalDateTime): LocalDateTime {
    return LocalDateTime(
        LocalDate(dateTime.year, dateTime.month.number, 1),
        LocalTime(0, 0)
    )
}

internal fun startOfWeek(dateTime: LocalDateTime): LocalDateTime {
    val shift = dateTime.date.dayOfWeek.isoDayNumber - 1
    val monday = dateTime.date.minus(DatePeriod(days = shift))
    return LocalDateTime(monday, LocalTime(0, 0))
}

internal fun minDT(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if (a <= b) a else b
internal fun maxDT(a: LocalDateTime, b: LocalDateTime): LocalDateTime = if (a >= b) a else b
