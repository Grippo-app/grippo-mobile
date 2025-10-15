package com.grippo.toolkit.date.utils.internal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

internal class FixedLocaleDateFormatter(
    private val base: DateFormatter,
    private val fixedTag: String?
) : DateFormatter {
    override fun format(value: LocalDateTime, pattern: String, localeTag: String?): String? {
        return base.format(value, pattern, fixedTag ?: localeTag)
    }

    override fun format(value: LocalDate, pattern: String, localeTag: String?): String? {
        return base.format(value, pattern, fixedTag ?: localeTag)
    }

    override fun format(value: LocalTime, pattern: String, localeTag: String?): String? {
        return base.format(value, pattern, fixedTag ?: localeTag)
    }
}