package com.grippo.state.domain.range

import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.toolkit.date.utils.DateRange

public fun DateRange.Range.toDomain(): Range? {
    return when (this) {
        is DateRange.Range.Daily -> Range.DAILY
        is DateRange.Range.Weekly -> Range.WEEKLY
        is DateRange.Range.Last7Days -> Range.LAST_7_DAYS
        is DateRange.Range.Last14Days -> Range.LAST_14_DAYS
        is DateRange.Range.Monthly -> Range.MONTHLY
        is DateRange.Range.Last30Days -> Range.LAST_30_DAYS
        is DateRange.Range.Last60Days -> Range.LAST_60_DAYS
        is DateRange.Range.Last365Days -> Range.LAST_365_DAYS
        is DateRange.Range.Yearly -> Range.YEARLY
        is DateRange.Range.Infinity -> Range.INFINITY
        is DateRange.Range.Undefined -> null
    }
}
