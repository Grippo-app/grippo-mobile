package com.grippo.date.utils.internal

import kotlinx.datetime.LocalDateTime

internal class FixedLocaleDateFormatter(
    private val base: DateFormatter,
    private val fixedTag: String?
) : DateFormatter {
    override fun format(value: LocalDateTime, pattern: String, localeTag: String?): String? {
        return base.format(value, pattern, fixedTag ?: localeTag)
    }
}