package com.grippo.toolkit.logger.internal

internal expect object LogFileReader {
    fun read(location: String): String
}
