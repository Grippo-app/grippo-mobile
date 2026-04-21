package com.grippo.toolkit.date.utils.internal

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
internal enum class DayTime(internal val localTime: LocalTime) {
    StartOfDay(localTime = LocalTime(hour = 0, minute = 0, second = 0, nanosecond = 0)),
    EndOfDay(localTime = LocalTime(hour = 23, minute = 59, second = 59, nanosecond = 999_999_900)),
}