package com.grippo.toolkit.calculation.internal

import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.w
import com.grippo.toolkit.calculation.models.Bucket
import com.grippo.toolkit.calculation.models.BucketScale
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime

// -------- Bucket labelers --------

internal suspend fun defaultLabeler(
    scale: BucketScale,
    stringProvider: StringProvider
): (Bucket) -> String {
    val w = stringProvider.get(Res.string.w)
    return when (scale) {
        BucketScale.DAY -> { b ->
            DateTimeUtils.format(b.start, DateFormat.WEEKDAY_SHORT)
        }

        BucketScale.WEEK -> { b ->
            "$w${isoWeekNumber(b.start)}-${
                DateTimeUtils.format(b.start, DateFormat.MONTH_SHORT)
            }"
        }

        BucketScale.MONTH -> { b ->
            DateTimeUtils.format(b.start, DateFormat.MONTH_SHORT)
        }

        BucketScale.EXERCISE -> { _ -> "" }
    }
}

internal suspend fun LocalDateTime.label(
    scale: BucketScale,
    stringProvider: StringProvider
): String {
    val w = stringProvider.get(Res.string.w)
    return when (scale) {
        BucketScale.DAY, BucketScale.EXERCISE -> {
            DateTimeUtils.format(this, DateFormat.DATE_DD_MMM) // e.g., "02 Sep"
        }

        BucketScale.WEEK -> {
            "$w${isoWeekNumber(this)}-${
                DateTimeUtils.format(this, DateFormat.MONTH_SHORT)
            }"
        }

        BucketScale.MONTH -> {
            DateTimeUtils.format(this, DateFormat.MONTH_SHORT)
        }
    }
}

// -------- Time labels for heatmaps --------

internal suspend fun defaultTimeLabels(
    buckets: List<Bucket>,
    scale: BucketScale,
    stringProvider: StringProvider
): List<String> {
    val w = stringProvider.get(Res.string.w)
    return when (scale) {
        BucketScale.DAY -> buckets.map {
            DateTimeUtils.format(it.start, DateFormat.DATE_DD_MMM) // e.g., "02 Sep"
        }

        BucketScale.WEEK -> buckets.map {
            "$w${isoWeekNumber(it.start)}-${
                DateTimeUtils.format(it.start, DateFormat.MONTH_SHORT)
            }" // e.g., "W36-Sep"
        }

        BucketScale.MONTH -> buckets.map {
            DateTimeUtils.format(it.start, DateFormat.MONTH_SHORT) // e.g., "Sep"
        }

        BucketScale.EXERCISE -> buckets.map {
            DateTimeUtils.format(it.start, DateFormat.DATE_DD_MMM)
        }
    }
}