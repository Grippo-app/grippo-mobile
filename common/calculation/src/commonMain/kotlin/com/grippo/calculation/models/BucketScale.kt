package com.grippo.calculation.models

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
internal enum class BucketScale {
    EXERCISE,
    DAY,
    WEEK,
    MONTH
}

internal data class Bucket(val start: LocalDateTime, val end: LocalDateTime)