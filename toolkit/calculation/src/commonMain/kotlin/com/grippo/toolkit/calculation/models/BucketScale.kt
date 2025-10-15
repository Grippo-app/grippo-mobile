package com.grippo.toolkit.calculation.models

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDateTime

@Immutable
internal enum class BucketScale {
    EXERCISE,
    DAY,
    WEEK,
    MONTH
}

@Immutable
internal data class Bucket(
    val start: LocalDateTime,
    val end: LocalDateTime
)