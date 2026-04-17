package com.grippo.domain.state.range

import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.toolkit.date.utils.DateRange

public fun Range.toState(): DateRange.Range {
    return when (this) {
        Range.DAILY -> DateRange.Range.Daily()
        Range.WEEKLY -> DateRange.Range.Weekly()
        Range.LAST_7_DAYS -> DateRange.Range.Last7Days()
        Range.LAST_14_DAYS -> DateRange.Range.Last14Days()
        Range.MONTHLY -> DateRange.Range.Monthly()
        Range.LAST_30_DAYS -> DateRange.Range.Last30Days()
        Range.LAST_60_DAYS -> DateRange.Range.Last60Days()
        Range.LAST_365_DAYS -> DateRange.Range.Last365Days()
        Range.YEARLY -> DateRange.Range.Yearly()
        Range.INFINITY -> DateRange.Range.Infinity()
    }
}
