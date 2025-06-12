package com.grippo.profile.weight.history

import com.grippo.core.models.BaseDirection

internal sealed interface WeightHistoryDirection : BaseDirection {
    data object Back : WeightHistoryDirection
}