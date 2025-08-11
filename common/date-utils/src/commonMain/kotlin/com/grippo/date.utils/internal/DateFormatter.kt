package com.grippo.date.utils.internal

import kotlinx.datetime.LocalDateTime

internal interface DateFormatter {
    fun format(value: LocalDateTime, pattern: String, localeTag: String? = null): String?
}