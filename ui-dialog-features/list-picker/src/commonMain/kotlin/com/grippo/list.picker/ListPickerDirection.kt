package com.grippo.list.picker

import com.grippo.core.models.BaseDirection

public sealed interface ListPickerDirection : BaseDirection {
    public data class BackWithResult(val id: String) : ListPickerDirection
    public data object Back : ListPickerDirection
}
