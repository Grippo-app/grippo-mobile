package com.grippo.profile.weight.history

import com.grippo.core.foundation.models.BaseDirection

internal sealed interface WeightHistoryDirection : BaseDirection {
    data object Back : WeightHistoryDirection
}