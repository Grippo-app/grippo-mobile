package com.grippo.toolkit.logger.internal

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

internal actual class LogFileWriter private constructor(
    private val filePath: String,
) {

    actual val location: String
        get() = filePath

    actual fun append(text: String) {
        ensureDirectoryExists()
        val file: CPointer<FILE>? = fopen(filePath, "ab")
        if (file == null) {
            return
        }
        val bytes = text.encodeToByteArray()
        bytes.usePinned { pinned ->
            fwrite(pinned.addressOf(0), 1uL, bytes.size.toULong(), file)
            Unit
        }
        fclose(file)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ensureDirectoryExists() {
        val manager = NSFileManager.defaultManager
        val directoryPath = filePath.substringBeforeLast('/', "")
        if (directoryPath.isEmpty()) return
        manager.createDirectoryAtPath(
            path = directoryPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }

    actual companion object {
        actual fun create(fileName: String): LogFileWriter {
            val baseDirectory = resolveBaseDirectory()
            val sanitizedBase = baseDirectory.trimEnd('/')
            val path = "$sanitizedBase/$fileName"
            return LogFileWriter(path)
        }

        actual fun deleteFile(path: String): Boolean {
            val manager = NSFileManager.defaultManager
            // removeItemAtPath returns true if file was removed
            return manager.removeItemAtPath(path, null)
        }

        @OptIn(ExperimentalForeignApi::class)
        private fun resolveBaseDirectory(): String {
            val manager = NSFileManager.defaultManager
            val tmpDirectory = NSTemporaryDirectory()
            val sanitizedTmp = tmpDirectory.trimEnd('/')
            val base = "$sanitizedTmp/grippo/logs"
            manager.createDirectoryAtPath(
                path = base,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
            return base
        }

        actual val DEFAULT_FILE_NAME: String = "app.log"
    }
}
