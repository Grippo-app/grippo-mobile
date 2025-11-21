package com.grippo.core.state.digest

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDate
import kotlin.time.Duration

@Immutable
public data class DailyDigestState(
    val date: LocalDate,
    val exercisesCount: Int,
    val trainingsCount: Int,
    val duration: Duration,
    val total: VolumeFormatState,
    val totalSets: Int,
)

public fun stubDailyDigest(): DailyDigestState = DailyDigestState(
    date = DateTimeUtils.now().date,
    exercisesCount = 5,
    duration = Duration.parse("2h"),
    total = VolumeFormatState.of(123F),
    totalSets = 5,
    trainingsCount = 3
)
