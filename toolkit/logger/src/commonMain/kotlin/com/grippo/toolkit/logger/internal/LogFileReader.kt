package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.LogCategory

internal expect object LogFileReader {
    fun read(location: String): String
    fun readGrouped(location: String): Map<LogCategory, List<String>>
}
