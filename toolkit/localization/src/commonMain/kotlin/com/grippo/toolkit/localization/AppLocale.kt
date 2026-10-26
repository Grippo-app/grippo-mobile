package com.grippo.toolkit.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.grippo.toolkit.localization.internal.LocaleSelection
import com.grippo.toolkit.localization.internal.SystemLocale

public object AppLocale {

    private var selection by mutableStateOf<LocaleSelection>(LocaleSelection.Pending)

    public val current: String
        @Composable get() {
            return selection.tag ?: SystemLocale.current
        }

    public val isResolved: Boolean
        get() = selection.isResolved

    public fun current(): String {
        return selection.tag ?: SystemLocale.current()
    }

    public fun update(tag: String?) {
        if (tag != null) {
            SystemLocale.apply(tag)
        }
        selection = LocaleSelection.of(tag)
    }

    internal fun reapply() {
        val tag = selection.tag ?: return
        SystemLocale.apply(tag)
    }
}
