package com.grippo.weight.picker

import com.grippo.core.models.BaseDirection

public sealed interface WeightPickerDirection : BaseDirection {
    public data object Dismiss : WeightPickerDirection
}