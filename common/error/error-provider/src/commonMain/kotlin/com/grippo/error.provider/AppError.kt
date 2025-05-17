package com.grippo.error.provider

public sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    public sealed class Network : AppError(message = "") {
        public data class Expected(
            val keys: List<String>,
            override val message: String,
        ) : Network()

        public data class Unexpected(
            val statusCode: Int,
            override val message: String,
        ) : Network()
    }

    public data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}