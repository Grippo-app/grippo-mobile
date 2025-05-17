package com.grippo.error.provider

public sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    public sealed class Network(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause) {

        public data class Expected(
            val keys: List<String> = emptyList(),
            override val message: String,
            override val cause: Throwable? = null
        ) : Network(message, cause)

        public data class Unexpected(
            val statusCode: Int? = null,
            override val message: String,
            override val cause: Throwable? = null
        ) : Network(message, cause)

        public data class NoInternet(
            override val message: String,
            override val cause: Throwable? = null
        ) : Network(message, cause)

        public data class Timeout(
            override val message: String,
            override val cause: Throwable? = null
        ) : Network(message, cause)

        public data class ConnectionLost(
            override val message: String,
            override val cause: Throwable? = null
        ) : Network(message, cause)
    }

    public data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
