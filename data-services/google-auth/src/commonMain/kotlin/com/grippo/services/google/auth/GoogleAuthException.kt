package com.grippo.services.google.auth

public sealed class GoogleAuthException(
    override val message: String,
    override val cause: Throwable? = null,
) : IllegalStateException(message, cause) {

    public class InvalidServerClientId(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)

    public class NoCredential(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)

    public class Cancelled(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)

    public class ReauthFailed(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)

    public class Interrupted(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)

    public class ProviderMisconfigured(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)

    public class CredentialManagerFailed(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)

    public class UnsupportedCredential(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)

    public class TokenParseFailed(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)

    public class Unknown(
        message: String,
        cause: Throwable? = null,
    ) : GoogleAuthException(message, cause)
}