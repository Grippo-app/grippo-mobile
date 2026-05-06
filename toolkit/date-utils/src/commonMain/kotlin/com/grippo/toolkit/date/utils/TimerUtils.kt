package com.grippo.toolkit.date.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDateTime
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

/**
 * Renders elapsed seconds as `HH:mm:ss` with hours unbounded — workouts that
 * exceed 24 hours render as `25:00:00` rather than wrapping to `01:00:00` (or
 * crashing on `LocalTime(hour = 24)`).
 *
 * Hours are zero-padded to at least two digits; longer hour strings (3+ digits)
 * grow naturally rather than overflow.
 */
private fun formatFromSeconds(totalSeconds: Long): String {
    val negative = totalSeconds < 0
    val s = abs(totalSeconds)

    val hours = s / 3_600
    val minutes = (s % 3_600) / 60
    val seconds = s % 60

    val core = buildString {
        append(hours.toString().padStart(2, '0'))
        append(':')
        append(minutes.toString().padStart(2, '0'))
        append(':')
        append(seconds.toString().padStart(2, '0'))
    }

    return if (negative) "-$core" else core
}
