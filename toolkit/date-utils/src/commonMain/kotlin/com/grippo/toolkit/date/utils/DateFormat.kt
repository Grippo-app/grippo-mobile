package com.grippo.toolkit.date.utils

import androidx.compose.runtime.Immutable

// https://kotlinlang.org/api/kotlinx-datetime/kotlinx-datetime/kotlinx.datetime.format/by-unicode-pattern.html
@Immutable
public enum class DateFormat(internal val value: String) {
    MONTH_FULL("MMMM"),
    TIME_24H_HH_MM("HH:mm"),
    TIME_24H_HH_MM_SS("HH:mm:ss"),
    TIME_24H_H_MM("H:mm"),
    DATE_MMM_DD_YYYY("MMM dd, yyyy"),
    DATE_MMM_DD_COMMA("MMM, dd"),
    DATE_DD_DOT_MM("dd.MM"),
    DATE_DD_MMM("dd MMM"),
    DATE_DD_MMMM("dd MMMM"),
    DATE_TIME_DD_MMM_HH_MM("MMM dd, HH:mm"),
    MONTH_SHORT("MMM"),
    WEEKDAY_SHORT("EEE"),
    WEEKDAY_LONG("EEEE")
}
