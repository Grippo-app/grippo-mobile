package com.grippo.height.picker

import com.grippo.core.models.BaseDirection

public sealed interface HeightPickerDirection : BaseDirection {
    public data class BackWithResult(val value: Int) : HeightPickerDirection
    public data object Back : HeightPickerDirection
}