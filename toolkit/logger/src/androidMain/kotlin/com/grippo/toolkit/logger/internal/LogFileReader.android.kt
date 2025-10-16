package com.grippo.toolkit.logger.internal

import com.grippo.toolkit.logger.internal.models.LogCategory
import java.io.File

internal actual object LogFileReader {

    actual fun read(location: String): String {
        val file = File(location)
        if (!file.exists() || !file.isFile) {
            return ""
        }
        return runCatching { file.readText() }
            .getOrElse { "" }
    }

    actual fun readGrouped(location: String): Map<LogCategory, List<String>> {
        val content = read(location)
        return LogFileParser.groupByCategory(content)
    }
}
