package com.grippo.date.utils

import androidx.compose.runtime.Immutable

// https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime.format/by-unicode-pattern.html
@Immutable
public enum class DateFormat(internal val value: String) {
    MMMM("MMMM"),
    HH_mm("HH:mm"),
    MMM_dd_yyyy("MMM dd, yyyy"),
    MMM_dd("MMM, dd")
}