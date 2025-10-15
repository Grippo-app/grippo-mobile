package com.grippo.toolkit.date.utils

import com.grippo.toolkit.date.utils.internal.DateFormatter
import com.grippo.toolkit.date.utils.internal.FixedLocaleDateFormatter
import com.grippo.toolkit.date.utils.internal.SystemDateFormatter
import com.grippo.toolkit.logger.AppLogger
import kotlin.concurrent.Volatile

public object DateFormatting {
    @Volatile
    internal var current: DateFormatter = SystemDateFormatter()
        private set

    @Volatile
    private var lastTag: String? = null

    // To handle locale change from device or manually
    public fun install(tag: String?) {
        if (lastTag == tag) return
        AppLogger.General.warning("\uD83C\uDF0D New locale >> $tag")
        lastTag = tag
        current = FixedLocaleDateFormatter(SystemDateFormatter(), tag)
    }
}
