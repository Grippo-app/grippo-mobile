package com.grippo.toolkit.logger.internal

import java.io.File

internal actual class LogFileWriter private constructor(private val file: File) {

    private val lock = Any()

    actual val location: String
        get() = file.absolutePath

    actual fun append(text: String) {
        synchronized(lock) {
            ensureParentExists()
            file.appendText(text)
        }
    }

    private fun ensureParentExists() {
        val parent = file.parentFile ?: return
        if (!parent.exists()) {
            parent.mkdirs()
        }
    }

    actual companion object {
        actual fun create(fileName: String): LogFileWriter {
            val baseDirectory = resolveBaseDirectory()
            val file = File(baseDirectory, fileName)
            return LogFileWriter(file)
        }

        private fun resolveBaseDirectory(): File {
            val home = System.getProperty("user.home")
            val tmp = System.getProperty("java.io.tmpdir")
            val root = when {
                !home.isNullOrBlank() -> File(home, "grippo/logs")
                !tmp.isNullOrBlank() -> File(tmp, "grippo/logs")
                else -> File("/tmp", "grippo/logs")
            }
            if (!root.exists()) {
                root.mkdirs()
            }
            return root
        }

        actual val DEFAULT_FILE_NAME: String = "app.log"
    }
}
