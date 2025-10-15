package com.grippo.logger.platform

internal expect fun getCallerLocation(depth: Int = 2): String
