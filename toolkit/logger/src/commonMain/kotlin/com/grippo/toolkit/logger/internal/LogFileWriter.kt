package com.grippo.toolkit.logger.internal

internal expect class LogFileWriter {
    val location: String

    fun append(text: String)

    companion object {

        fun create(fileName: String = DEFAULT_FILE_NAME): LogFileWriter

        fun deleteFile(path: String): Boolean

        val DEFAULT_FILE_NAME: String
    }
}
