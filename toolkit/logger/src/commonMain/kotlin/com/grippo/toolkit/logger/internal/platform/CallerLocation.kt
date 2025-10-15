package com.grippo.toolkit.logger.internal.platform

internal expect fun getCallerLocation(depth: Int = 2): String
