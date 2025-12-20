package com.grippo.core.state.trainings.digest

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.time.Duration

@Immutable
public data class WeeklyDigestState(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val exercisesCount: Int,
    val trainingsCount: Int,
    val duration: Duration,
    val total: VolumeFormatState,
    val totalSets: Int,
)

public fun stubWeeklyDigest(): WeeklyDigestState = WeeklyDigestState(
    weekStart = DateTimeUtils.now().date.minus(DatePeriod(days = 6)),
    weekEnd = DateTimeUtils.now().date,
    exercisesCount = 28,
    duration = Duration.parse("7h"),
    total = VolumeFormatState.of(540F),
    totalSets = 32,
    trainingsCount = 5,
)