package com.grippo.toolkit.share.internal

internal const val PNG_EXTENSION: String = ".png"

internal fun requirePngFileName(fileName: String) {
    val isPlainPngName = fileName.length > PNG_EXTENSION.length &&
        fileName.endsWith(PNG_EXTENSION) &&
        '/' !in fileName &&
        ".." !in fileName

    require(isPlainPngName) {
        "fileName must be a plain file name ending in $PNG_EXTENSION: $fileName"
    }
}
