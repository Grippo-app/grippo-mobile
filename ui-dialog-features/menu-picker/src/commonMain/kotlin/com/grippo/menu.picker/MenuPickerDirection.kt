package com.grippo.menu.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.menu.PickerMenuItem

public sealed interface MenuPickerDirection : BaseDirection {
    public data class BackWithResult(val item: PickerMenuItem) : MenuPickerDirection
    public data object Back : MenuPickerDirection
}
