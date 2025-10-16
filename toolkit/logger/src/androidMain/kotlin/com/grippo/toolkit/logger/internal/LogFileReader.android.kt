package com.grippo.toolkit.logger.internal

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
}
