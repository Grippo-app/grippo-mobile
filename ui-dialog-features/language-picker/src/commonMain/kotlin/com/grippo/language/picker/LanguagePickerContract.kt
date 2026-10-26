package com.grippo.language.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.settings.LanguageEnumState

@Immutable
internal interface LanguagePickerContract {
    fun onSelectLanguage(language: LanguageEnumState)
    fun onDismiss()

    @Immutable
    companion object Empty : LanguagePickerContract {
        override fun onSelectLanguage(language: LanguageEnumState) {}
        override fun onDismiss() {}
    }
}
