package com.grippo.calculation.internal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

internal fun isoWeekNumber(weekStartMonday: LocalDateTime): Int {
    val date = weekStartMonday.date
    val firstJan = LocalDate(date.year, 1, 1)
    val doy = (date.toEpochDays() - firstJan.toEpochDays()).toInt() + 1
    return ((doy - 1) / 7) + 1
}