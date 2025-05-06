package com.grippo.weight.picker

import com.grippo.core.models.BaseDirection

public sealed interface WeightPickerDirection : BaseDirection {
    public data class DismissWithResult(val value: Float) : WeightPickerDirection
}