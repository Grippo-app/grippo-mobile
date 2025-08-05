package com.grippo.date.utils

import androidx.compose.runtime.Immutable

// https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime.format/by-unicode-pattern.html
@Immutable
public enum class DateFormat(internal val value: String) {
    HH_mm("HH:mm"),
    uuuu_MM_d("uuuu, MM d"),
    MM_d("MM, d")
}