package com.grippo.state.domain.range

import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.toolkit.date.utils.DateRangeKind

public fun DateRangeKind.toDomain(): Range? {
    return when (this) {
        DateRangeKind.Daily -> Range.DAILY
        DateRangeKind.Weekly -> Range.WEEKLY
        DateRangeKind.Last7Days -> Range.LAST_7_DAYS
        DateRangeKind.Last14Days -> Range.LAST_14_DAYS
        DateRangeKind.Monthly -> Range.MONTHLY
        DateRangeKind.Last30Days -> Range.LAST_30_DAYS
        DateRangeKind.Last60Days -> Range.LAST_60_DAYS
        DateRangeKind.Last365Days -> Range.LAST_365_DAYS
        DateRangeKind.Yearly -> Range.YEARLY
        DateRangeKind.Infinity -> Range.INFINITY
        DateRangeKind.Custom -> null
    }
}
