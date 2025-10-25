package com.grippo.toolkit.logger.internal

internal expect class LogFileWriter {
    val location: String

    fun append(text: String)

    companion object {

        /**
         * Creates writer instance pointing at [fileName] in platform-specific logs dir.
         */
        fun create(fileName: String = DEFAULT_FILE_NAME): LogFileWriter

        /**
         * Physically deletes file at [path] if it exists.
         *
         * @return true if the file was deleted, false otherwise
         */
        fun deleteFile(path: String): Boolean

        val DEFAULT_FILE_NAME: String
    }
}
