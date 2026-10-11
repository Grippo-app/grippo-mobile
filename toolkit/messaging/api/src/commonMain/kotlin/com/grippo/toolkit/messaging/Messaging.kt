package com.grippo.toolkit.messaging

import kotlinx.coroutines.flow.StateFlow

/** Opaque secret; toString must never reveal its contents. */
public class PushToken(public val value: String) {
    init {
        require(value.length in 1..4096 && value.none { it.isWhitespace() || it.isISOControl() })
    }

    override fun equals(other: Any?): Boolean = other is PushToken && value == other.value
    override fun hashCode(): Int = value.hashCode()
    override fun toString(): String = "PushToken(redacted)"
}

public enum class TokenUnavailableReason {
    NotInitialized,
    PlatformNotReady,
    Closed
}

public enum class MessagingFailure {
    SdkFailure,
    InvalidToken
}

public sealed interface PushTokenResult {
    public class Available(public val token: PushToken) : PushTokenResult
    public class Unavailable(public val reason: TokenUnavailableReason) : PushTokenResult
    public class Failed(public val reason: MessagingFailure) : PushTokenResult
}

public enum class TokenSyncStatus {
    Idle,
    Pending,
    Synced,
    Failed,
    Closed
}

public interface Messaging {
    public val token: StateFlow<PushTokenResult>
    public val syncStatus: StateFlow<TokenSyncStatus>
    public suspend fun getToken(): PushTokenResult
}

/** Platform callbacks enter the core separately from the feature API. */
public interface MessagingSink {
    public suspend fun getToken(): PushTokenResult
}

public interface PushTokenHandler {
    public suspend fun onTokenRefresh(token: PushToken)
}

/** Validates untrusted platform values without throwing across the Swift boundary. */
public object PushTokenDecoder {
    public fun decode(value: String): PushTokenResult = try {
        PushTokenResult.Available(PushToken(value))
    } catch (_: IllegalArgumentException) {
        PushTokenResult.Failed(MessagingFailure.InvalidToken)
    }
}
