package com.grippo.toolkit.messaging.token

import com.grippo.toolkit.messaging.internal.PushTokenRules

/** Opaque secret; toString must never reveal its contents. */
public class PushToken(public val value: String) {
    init {
        require(PushTokenRules.isValid(value))
    }

    override fun equals(other: Any?): Boolean = other is PushToken && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = PushTokenRules.REDACTED_DESCRIPTION
}
