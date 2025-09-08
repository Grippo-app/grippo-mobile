package com.grippo.calculation.models

import kotlinx.datetime.LocalDateTime

// Keep your enum & data class
internal enum class BucketScale {
    EXERCISE,
    DAY,
    WEEK,
    MONTH
}

internal data class Bucket(val start: LocalDateTime, val end: LocalDateTime)