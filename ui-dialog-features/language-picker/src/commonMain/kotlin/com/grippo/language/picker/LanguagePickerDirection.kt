package com.grippo.language.picker

import com.grippo.core.foundation.models.BaseDirection
import com.grippo.core.state.settings.LanguageEnumState

public sealed interface LanguagePickerDirection : BaseDirection {
    public data class BackWithResult(val value: LanguageEnumState) : LanguagePickerDirection
    public data object Back : LanguagePickerDirection
}
