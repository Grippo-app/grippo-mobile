package com.grippo.toolkit.date.utils.internal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

internal interface DateFormatter {
    fun format(value: LocalDateTime, pattern: String, localeTag: String? = null): String?
    fun format(value: LocalDate, pattern: String, localeTag: String? = null): String?
    fun format(value: LocalTime, pattern: String, localeTag: String? = null): String?
}