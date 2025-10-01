package com.grippo.menu.picker

import com.grippo.core.models.BaseDirection

public sealed interface MenuPickerDirection : BaseDirection {
    public data class BackWithResult(val id: String) : MenuPickerDirection
    public data object Back : MenuPickerDirection
}
