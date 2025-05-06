package com.grippo.height.picker

import com.grippo.core.models.BaseDirection

public sealed interface HeightPickerDirection : BaseDirection {
    public data object Dismiss : HeightPickerDirection
    public data class DismissWithResult(val value: Int) : HeightPickerDirection
}