package com.grippo.date.utils

import com.grippo.date.utils.platform.SystemDateFormatter
import kotlinx.datetime.LocalDateTime
import kotlin.concurrent.Volatile

internal interface DateFormatter {
    fun format(value: LocalDateTime, pattern: String, localeTag: String? = null): String?
}

internal class FixedLocaleDateFormatter(
    private val base: DateFormatter,
    private val fixedTag: String?
) : DateFormatter {
    override fun format(value: LocalDateTime, pattern: String, localeTag: String?): String? {
        return base.format(value, pattern, fixedTag ?: localeTag)
    }
}

public object DateFormatting {
    @Volatile
    internal var current: DateFormatter = SystemDateFormatter()
        private set

    @Volatile
    private var lastTag: String? = null

    public fun install(tag: String?) {
        if (lastTag == tag) return
        lastTag = tag
        current = FixedLocaleDateFormatter(SystemDateFormatter(), tag)
    }
}