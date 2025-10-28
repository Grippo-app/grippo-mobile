package com.grippo.toolkit.date.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

public fun timerTextFlow(
    start: LocalDateTime,
): Flow<String> = flow {
    val nowMs = DateTimeUtils.asInstant(DateTimeUtils.now()).toEpochMilliseconds()
    val startMs = DateTimeUtils.asInstant(start).toEpochMilliseconds()
    val baseSeconds = (nowMs - startMs) / 1000L
    emit(formatFromSeconds(baseSeconds))

    val mark = TimeSource.Monotonic.markNow()

    while (true) {
        val elapsedMs = mark.elapsedNow().inWholeMilliseconds
        val toNextMs = 1000L - (elapsedMs % 1000L)
        delay(toNextMs.milliseconds)

        val seconds = mark.elapsedNow().inWholeMilliseconds / 1000L
        emit(formatFromSeconds(baseSeconds + seconds))
    }
}

// Builds LocalTime from total seconds and optionally reports overflow days and sign.
private fun toLocalTime(
    totalSeconds: Long,
    onOverflowDays: ((days: Int, negative: Boolean) -> Unit)? = null
): LocalTime {
    val negative = totalSeconds < 0
    val s = abs(totalSeconds)

    val days = (s / 86_400).toInt()
    val hours = ((s % 86_400) / 3_600).toInt()
    val minutes = ((s % 3_600) / 60).toInt()
    val seconds = (s % 60).toInt()

    onOverflowDays?.invoke(days, negative)

    return LocalTime(
        hour = hours,
        minute = minutes,
        second = seconds
    )
}

// Formats from total seconds directly (no extra types).
private fun formatFromSeconds(totalSeconds: Long): String {
    var negative = false
    val time = toLocalTime(totalSeconds) { _, neg ->
        negative = neg
    }

    val core = DateTimeUtils.format(time, DateFormat.TIME_24H_HH_MM)

    return if (negative) "-$core" else core
}