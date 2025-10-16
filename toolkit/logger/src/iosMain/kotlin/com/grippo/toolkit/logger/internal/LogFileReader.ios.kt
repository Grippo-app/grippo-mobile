package com.grippo.toolkit.logger.internal

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

internal actual object LogFileReader {

    actual fun read(location: String): String {
        val manager = NSFileManager.defaultManager
        val exists = manager.fileExistsAtPath(location)
        if (!exists) return ""

        return memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val content = NSString.stringWithContentsOfFile(
                path = location,
                encoding = NSUTF8StringEncoding,
                error = errorPtr.ptr
            )
            content ?: ""
        }
    }
}
