package com.grippo.date.utils

import com.grippo.date.utils.internal.DateFormatter
import com.grippo.date.utils.internal.FixedLocaleDateFormatter
import com.grippo.date.utils.platform.SystemDateFormatter
import com.grippo.logger.AppLogger
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