package com.grippo.toolkit.date.utils

import com.grippo.toolkit.date.utils.internal.DateFormatter
import com.grippo.toolkit.date.utils.internal.FixedLocaleDateFormatter
import com.grippo.toolkit.date.utils.internal.SystemDateFormatter
import kotlin.concurrent.Volatile

public object DateFormatting {
    @Volatile
    internal var current: DateFormatter = SystemDateFormatter()
        private set

    @Volatile
    private var lastTag: String? = null

    public fun install(tag: String?) {
        val normalizedTag = tag?.split(',')?.firstOrNull { it.isNotBlank() }?.trim()
        if (lastTag == normalizedTag) {
            return
        }

        lastTag = normalizedTag
        current = FixedLocaleDateFormatter(base = SystemDateFormatter(), fixedTag = normalizedTag)
    }
}
