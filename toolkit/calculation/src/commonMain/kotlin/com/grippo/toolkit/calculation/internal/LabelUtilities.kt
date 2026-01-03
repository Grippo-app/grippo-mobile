package com.grippo.toolkit.calculation.internal

import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.providers.StringProvider
import com.grippo.design.resources.provider.w
import com.grippo.toolkit.calculation.models.BucketScale
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.datetime.LocalDateTime

internal suspend fun LocalDateTime.label(
    scale: BucketScale,
    stringProvider: StringProvider
): String {
    val w = stringProvider.get(Res.string.w)
    return when (scale) {
        BucketScale.DAY, BucketScale.EXERCISE -> {
            DateTimeUtils.format(this, DateFormat.DateOnly.DateDdMmm) // e.g., "02 Sep"
        }

        BucketScale.WEEK -> {
            "$w${isoWeekNumber(this)}-${
                DateTimeUtils.format(this, DateFormat.DateOnly.MonthShort)
            }"
        }

        BucketScale.MONTH -> {
            DateTimeUtils.format(this, DateFormat.DateOnly.MonthShort)
        }
    }
}
