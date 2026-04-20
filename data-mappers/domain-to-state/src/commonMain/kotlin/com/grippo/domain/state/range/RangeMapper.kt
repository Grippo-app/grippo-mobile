package com.grippo.domain.state.range

import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.toolkit.date.utils.DateRangeKind

public fun Range.toState(): DateRangeKind {
    return when (this) {
        Range.DAILY -> DateRangeKind.Daily
        Range.WEEKLY -> DateRangeKind.Weekly
        Range.LAST_7_DAYS -> DateRangeKind.Last7Days
        Range.LAST_14_DAYS -> DateRangeKind.Last14Days
        Range.MONTHLY -> DateRangeKind.Monthly
        Range.LAST_30_DAYS -> DateRangeKind.Last30Days
        Range.LAST_60_DAYS -> DateRangeKind.Last60Days
        Range.LAST_365_DAYS -> DateRangeKind.Last365Days
        Range.YEARLY -> DateRangeKind.Yearly
        Range.INFINITY -> DateRangeKind.Infinity
    }
}
