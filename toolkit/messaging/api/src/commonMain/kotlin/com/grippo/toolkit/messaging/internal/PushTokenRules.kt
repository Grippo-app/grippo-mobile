package com.grippo.toolkit.messaging.internal

internal object PushTokenRules {
    const val MAXIMUM_LENGTH = 4096
    const val REDACTED_DESCRIPTION = "PushToken(redacted)"

    fun isValid(rawToken: String): Boolean =
        rawToken.length in 1..MAXIMUM_LENGTH &&
            rawToken.none { it.isWhitespace() || it.isISOControl() }
}
