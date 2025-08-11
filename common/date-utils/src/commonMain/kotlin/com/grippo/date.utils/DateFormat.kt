package com.grippo.date.utils

import androidx.compose.runtime.Immutable

// https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime.format/by-unicode-pattern.html
@Immutable
public enum class DateFormat(internal val value: String) {
    MMMM("MMMM"),
    HH_mm("HH:mm"),
    uuuu_MMM_d("uuuu, MMM d"),
    MMM_d("MMM, d")
}