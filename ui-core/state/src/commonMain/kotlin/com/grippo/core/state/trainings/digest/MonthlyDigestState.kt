package com.grippo.core.state.trainings.digest

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

@Immutable
public data class MonthlyDigestState(
    val month: LocalDate,
    val exercisesCount: Int,
    val trainingsCount: Int,
    val duration: Duration,
    val total: VolumeFormatState,
    val totalSets: Int,
)

public fun stubMonthlyDigest(): MonthlyDigestState = MonthlyDigestState(
    month = DateTimeUtils.now().date,
    exercisesCount = 112,
    duration = Duration.parse("28h"),
    total = VolumeFormatState.of(2_450F),
    totalSets = 140,
    trainingsCount = 18,
)