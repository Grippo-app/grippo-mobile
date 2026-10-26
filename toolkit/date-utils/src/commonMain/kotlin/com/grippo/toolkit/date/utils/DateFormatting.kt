package com.grippo.toolkit.date.utils

import com.grippo.toolkit.date.utils.internal.DateFormatter
import com.grippo.toolkit.date.utils.internal.FixedLocaleDateFormatter
import com.grippo.toolkit.date.utils.internal.SystemDateFormatter
import com.grippo.toolkit.localization.AppLocale

internal object DateFormatting {
    private val system: DateFormatter = SystemDateFormatter()

    val current: DateFormatter
        get() {
            val tag = AppLocale.current().split(',').firstOrNull { it.isNotBlank() }?.trim()
            return FixedLocaleDateFormatter(base = system, fixedTag = tag)
        }
}
