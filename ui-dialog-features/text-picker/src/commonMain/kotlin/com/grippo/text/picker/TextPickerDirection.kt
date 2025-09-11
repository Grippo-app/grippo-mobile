package com.grippo.text.picker

import com.grippo.core.models.BaseDirection
import com.grippo.state.text.TextWithId

public sealed interface TextPickerDirection : BaseDirection {
    public data class BackWithResult(val value: TextWithId) : TextPickerDirection
    public data object Back : TextPickerDirection
}