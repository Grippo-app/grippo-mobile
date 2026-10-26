package com.grippo.toolkit.localization.internal

internal sealed interface LocaleSelection {
    val tag: String?
        get() = null
    val isResolved: Boolean
        get() = this !is Pending

    data object Pending : LocaleSelection
    data object Platform : LocaleSelection
    data class App(override val tag: String) : LocaleSelection

    companion object {
        fun of(tag: String?): LocaleSelection {
            return if (tag == null) Platform else App(tag)
        }
    }
}
