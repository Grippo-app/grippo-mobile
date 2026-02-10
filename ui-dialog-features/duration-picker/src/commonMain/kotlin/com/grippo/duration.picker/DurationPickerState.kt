package com.grippo.duration.picker

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.max
import kotlin.time.Duration

private const val DefaultMaxHours: Int = 23

internal val DefaultDurationMinutes: PersistentList<Int> =
    (0..59).toPersistentList()

internal fun resolveDurationHours(value: Duration): PersistentList<Int> {
    val hours = value.inWholeHours.toInt().coerceAtLeast(0)
    val maxHours = max(DefaultMaxHours, hours)
    return (0..maxHours).toPersistentList()
}

@Immutable
public data class DurationPickerState(
    val hours: PersistentList<Int>,
    val minutes: PersistentList<Int> = DefaultDurationMinutes,
    val value: Duration,
)
